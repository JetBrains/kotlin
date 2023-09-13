/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData.SymbolKind
import org.jetbrains.kotlin.backend.common.serialization.kind
import org.jetbrains.kotlin.fir.backend.generators.FakeOverrideGenerator
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirIrProvider(val components: Fir2IrComponents) : IrProvider {
    private val symbolProvider = components.session.symbolProvider
    private val declarationStorage = components.declarationStorage
    private val classifierStorage = components.classifierStorage
    private val fakeOverrideGenerator = FakeOverrideGenerator(components, Fir2IrConversionScope(components.configuration))

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {
        val signature = symbol.signature ?: return null
        return getDeclarationForSignature(signature, symbol.kind())
    }

    private fun getDeclarationForSignature(signature: IdSignature, kind: SymbolKind): IrDeclaration? = when (signature) {
        is IdSignature.AccessorSignature -> getDeclarationForAccessorSignature(signature)
        is IdSignature.CompositeSignature -> getDeclarationForCompositeSignature(signature, kind)
        is IdSignature.CommonSignature -> getDeclarationForCommonSignature(signature, kind)
        else -> error("Unexpected signature kind: $signature")
    }

    private fun getDeclarationForAccessorSignature(signature: IdSignature.AccessorSignature): IrDeclaration? {
        val property = getDeclarationForSignature(signature.propertySignature, SymbolKind.PROPERTY_SYMBOL) as? IrProperty ?: return null
        return when (signature.accessorSignature.shortName) {
            property.getter?.name?.asString() -> property.getter
            property.setter?.name?.asString() -> property.setter
            else -> null
        }
    }

    private fun getDeclarationForCompositeSignature(signature: IdSignature.CompositeSignature, kind: SymbolKind): IrDeclaration? {
        if (kind == SymbolKind.TYPE_PARAMETER_SYMBOL) {
            val container = (getDeclarationForSignature(signature.container, SymbolKind.CLASS_SYMBOL)
                ?: getDeclarationForSignature(signature.container, SymbolKind.FUNCTION_SYMBOL)
                ?: getDeclarationForSignature(signature.container, SymbolKind.PROPERTY_SYMBOL)
                    ) as IrTypeParametersContainer
            val localSignature = signature.inner as IdSignature.LocalSignature
            return container.typeParameters[localSignature.index()]
        }
        return getDeclarationForSignature(signature.nearestPublicSig(), kind)
    }

    private fun getDeclarationForCommonSignature(signature: IdSignature.CommonSignature, kind: SymbolKind): IrDeclaration? {
        val packageFqName = FqName(signature.packageFqName)
        val nameSegments = signature.nameSegments
        val topName = Name.identifier(nameSegments[0])

        val packageFragment = declarationStorage.getIrExternalPackageFragment(packageFqName)

        val firCandidates: List<FirDeclaration>
        val parent: IrDeclarationParent
        if (nameSegments.size == 1 && kind != SymbolKind.CLASS_SYMBOL) {
            firCandidates = symbolProvider.getTopLevelCallableSymbols(packageFqName, topName).map { it.fir }
            parent = packageFragment // TODO: need to insert file facade class on JVM
        } else {
            val topLevelClass = symbolProvider.getClassLikeSymbolByClassId(ClassId(packageFqName, topName))?.fir as? FirRegularClass
                ?: return null
            var firParentClass: FirRegularClass? = null
            var firClass = topLevelClass
            val midSegments = if (kind == SymbolKind.CLASS_SYMBOL) nameSegments.drop(1) else nameSegments.drop(1).dropLast(1)
            for (midName in midSegments) {
                firParentClass = firClass
                firClass = firClass.declarations.singleOrNull { (it as? FirRegularClass)?.name?.asString() == midName } as? FirRegularClass
                    ?: return null
            }
            val classId = firClass.classId
            val scope = with(components) { firClass.unsubstitutedScope() }

            fun findIrClass(firClass: FirRegularClass): IrClass {
                val irClassSymbol = classifierStorage.getOrCreateIrClass(firClass.symbol).symbol
                return getDeclaration(irClassSymbol) as IrClass
            }

            when (kind) {
                SymbolKind.CLASS_SYMBOL -> {
                    firCandidates = listOf(firClass)
                    parent = firParentClass?.let { findIrClass(it) } ?: packageFragment
                }
                SymbolKind.ENUM_ENTRY_SYMBOL -> {
                    val lastName = Name.guessByFirstCharacter(nameSegments.last())
                    val firCandidate = firClass.declarations.singleOrNull { (it as? FirEnumEntry)?.name == lastName }
                    firCandidates = firCandidate?.let { listOf(it) } ?: return null
                    parent = findIrClass(firClass)
                }
                SymbolKind.CONSTRUCTOR_SYMBOL -> {
                    val constructors = mutableListOf<FirConstructor>()
                    scope.processDeclaredConstructors { constructors.add(it.fir) }
                    firCandidates = constructors
                    parent = findIrClass(firClass)
                }
                SymbolKind.FUNCTION_SYMBOL -> {
                    parent = findIrClass(firClass)
                    val lastName = Name.guessByFirstCharacter(nameSegments.last())
                    val functions = mutableListOf<FirSimpleFunction>()
                    scope.processFunctionsByName(lastName) { functionSymbol ->
                        val dispatchReceiverClassId = (functionSymbol.fir.dispatchReceiverType as? ConeClassLikeType)?.lookupTag?.classId
                        val function = if (dispatchReceiverClassId != null && dispatchReceiverClassId != classId) {
                            fakeOverrideGenerator.createFirFunctionFakeOverride(firClass, parent, functionSymbol, scope)!!.first
                        } else functionSymbol.fir
                        functions.add(function)
                    }
                    firClass.staticScopeForCallables?.processFunctionsByName(lastName) { functionSymbol ->
                        functions.add(functionSymbol.fir)
                    }
                    firCandidates = functions
                }
                SymbolKind.PROPERTY_SYMBOL -> {
                    parent = findIrClass(firClass)
                    val lastName = Name.guessByFirstCharacter(nameSegments.last())
                    val properties = mutableListOf<FirVariable>()
                    scope.processPropertiesByName(lastName) { propertySymbol ->
                        propertySymbol as FirPropertySymbol
                        val dispatchReceiverClassId = (propertySymbol.fir.dispatchReceiverType as? ConeClassLikeType)?.lookupTag?.classId
                        val property = if (dispatchReceiverClassId != null && dispatchReceiverClassId != classId) {
                            fakeOverrideGenerator.createFirPropertyFakeOverride(firClass, parent, propertySymbol, scope)!!.first
                        } else propertySymbol.fir
                        properties.add(property)
                    }
                    firCandidates = properties
                }
                SymbolKind.FIELD_SYMBOL -> {
                    parent = findIrClass(firClass)
                    val lastName = Name.guessByFirstCharacter(nameSegments.last())
                    val fields = mutableListOf<FirVariable>()
                    scope.processPropertiesByName(lastName) { propertySymbol ->
                        fields.add(propertySymbol.fir)
                    }
                    firClass.staticScopeForCallables?.processPropertiesByName(lastName) { propertySymbol ->
                        fields.add(propertySymbol.fir)
                    }
                    firCandidates = fields
                }
                else -> {
                    val lastName = nameSegments.last()
                    firCandidates = firClass.declarations.filter { it is FirCallableDeclaration && it.symbol.name.asString() == lastName }
                    parent = findIrClass(firClass)
                }
            }
        }

        val firDeclaration = findDeclarationByHash(firCandidates, signature.id)
            ?: return null

        return when (kind) {
            SymbolKind.CLASS_SYMBOL -> {
                classifierStorage.getOrCreateIrClass((firDeclaration as FirRegularClass).symbol)
            }
            SymbolKind.ENUM_ENTRY_SYMBOL -> classifierStorage.getOrCreateIrEnumEntry(
                firDeclaration as FirEnumEntry, parent as IrClass
            )
            SymbolKind.CONSTRUCTOR_SYMBOL -> {
                val firConstructor = firDeclaration as FirConstructor
                declarationStorage.getOrCreateIrConstructor(firConstructor, parent as IrClass)
            }
            SymbolKind.FUNCTION_SYMBOL -> {
                val firSimpleFunction = firDeclaration as FirSimpleFunction
                declarationStorage.getOrCreateIrFunction(firSimpleFunction, parent)
            }
            SymbolKind.PROPERTY_SYMBOL -> {
                val firProperty = firDeclaration as FirProperty
                declarationStorage.getOrCreateIrProperty(firProperty, parent)
            }
            SymbolKind.FIELD_SYMBOL -> {
                val firField = firDeclaration as FirField
                declarationStorage.getOrCreateIrField(firField.symbol)
            }
            else -> error("Don't know how to deal with this symbol kind: $kind")
        }
    }

    private fun findDeclarationByHash(candidates: Collection<FirDeclaration>, hash: Long?): FirDeclaration? =
        candidates.firstOrNull { candidate ->
            if (hash == null) {
                // We don't compute id for type aliases and classes.
                candidate is FirClass || candidate is FirEnumEntry || candidate is FirTypeAlias
            } else {
                // The next line should have singleOrNull, but in some cases we get multiple references to the same FIR declaration.
                with(components.signatureComposer.mangler) { candidate.signatureMangle(compatibleMode = false) == hash }
            }
        }

    private val FirClass.staticScopeForCallables: FirScope?
        get() = scopeProvider.getStaticMemberScopeForCallables(this, components.session, components.scopeSession)
}
