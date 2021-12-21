/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData.SymbolKind
import org.jetbrains.kotlin.backend.common.serialization.kind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(SymbolInternals::class)
class FirIrProvider(val fir2IrComponents: Fir2IrComponents) : IrProvider {
    private val symbolProvider = fir2IrComponents.session.symbolProvider
    private val declarationStorage = fir2IrComponents.declarationStorage
    private val classifierStorage = fir2IrComponents.classifierStorage

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
            TODO()
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
            var firParentClass: FirRegularClass? = null
            var firClass = symbolProvider.getClassLikeSymbolByClassId(ClassId(packageFqName, topName))?.fir as? FirRegularClass
                ?: return null
            val midSegments = if (kind == SymbolKind.CLASS_SYMBOL) nameSegments.drop(1) else nameSegments.drop(1).dropLast(1)
            for (midName in midSegments) {
                firParentClass = firClass
                firClass = firClass.declarations.singleOrNull { (it as? FirRegularClass)?.name?.asString() == midName } as? FirRegularClass
                    ?: return null
            }
            val scope =
                firClass.unsubstitutedScope(fir2IrComponents.session, fir2IrComponents.scopeSession, withForcedTypeCalculator = true)
            when (kind) {
                SymbolKind.CLASS_SYMBOL -> {
                    firCandidates = listOf(firClass)
                    parent = firParentClass?.let { classifierStorage.getIrClassSymbol(it.symbol).owner } ?: packageFragment
                }
                SymbolKind.CONSTRUCTOR_SYMBOL -> {
                    val constructors = mutableListOf<FirConstructor>()
                    scope.processDeclaredConstructors { constructors.add(it.fir) }
                    firCandidates = constructors
                    parent = classifierStorage.getIrClassSymbol(firClass.symbol).owner
                }
                SymbolKind.FUNCTION_SYMBOL -> {
                    val lastName = Name.guessByFirstCharacter(nameSegments.last())
                    val functions = mutableListOf<FirSimpleFunction>()
                    scope.processFunctionsByName(lastName) { functions.add(it.fir) }
                    firCandidates = functions
                    parent = classifierStorage.getIrClassSymbol(firClass.symbol).owner
                }
                SymbolKind.PROPERTY_SYMBOL -> {
                    val lastName = Name.guessByFirstCharacter(nameSegments.last())
                    val properties = mutableListOf<FirVariable>()
                    scope.processPropertiesByName(lastName) { properties.add(it.fir) }
                    firCandidates = properties
                    parent = classifierStorage.getIrClassSymbol(firClass.symbol).owner
                }
                else -> {
                    val lastName = nameSegments.last()
                    firCandidates = firClass.declarations.filter { it is FirCallableDeclaration && it.symbol.name.asString() == lastName }
                    parent = classifierStorage.getIrClassSymbol(firClass.symbol).owner
                }
            }
        }

        // The next line should have singleOrNull, but in some cases we get multiple references to the same FIR declaration.
        val firDeclaration = firCandidates.firstOrNull { fir2IrComponents.signatureComposer.composeSignature(it) == signature }
            ?: return null

        return when (kind) {
            SymbolKind.CLASS_SYMBOL -> classifierStorage.getIrClassSymbol((firDeclaration as FirRegularClass).symbol).owner
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
                declarationStorage.getOrCreateIrPropertyByPureField(firField, parent)
            }
            else -> error("Don't know how to deal with this symbol kind: $kind")
        }
    }
}
