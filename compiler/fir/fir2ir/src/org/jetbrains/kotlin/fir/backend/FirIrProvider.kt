/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@OptIn(SymbolInternals::class)
class FirIrProvider(val fir2IrComponents: Fir2IrComponents) : IrProvider {
    private val symbolProvider = fir2IrComponents.session.symbolProvider

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {
        val declarationStorage = fir2IrComponents.declarationStorage
        val classifierStorage = fir2IrComponents.classifierStorage

        val signature = symbol.signature ?: return null

        val commonSignature = when(signature) {
            is IdSignature.CommonSignature -> signature
            is IdSignature.AccessorSignature -> signature.accessorSignature
            else ->
                TODO("Unknown signature type")
        }

        val packageFqName = FqName(commonSignature.packageFqName)
        val nameSegments = commonSignature.nameSegments
        val topName = Name.identifier(nameSegments[0])

        val packageFragment = declarationStorage.getIrExternalPackageFragment(packageFqName)

        val firCandidates: List<FirDeclaration>
        val parent: IrDeclarationParent
        if (nameSegments.size == 1 && symbol !is IrClassSymbol) {
            firCandidates = symbolProvider.getTopLevelCallableSymbols(packageFqName, topName).map { it.fir }
            parent = packageFragment // TODO: need to insert file facade class on JVM
        } else {
            var firParentClass: FirRegularClass? = null
            var firClass = symbolProvider.getClassLikeSymbolByClassId(ClassId(packageFqName, topName))?.fir as? FirRegularClass ?: return null
            val midSegments = if (symbol is IrClassSymbol) nameSegments.drop(1) else nameSegments.drop(1).dropLast(1)
            for (midName in midSegments) {
                firParentClass = firClass
                firClass = firClass.declarations.singleOrNull { (it as? FirRegularClass)?.name?.asString() == midName } as? FirRegularClass
                    ?: return null
            }
            if (symbol is IrClassSymbol) {
                firCandidates = listOf(firClass)
                parent = classifierStorage.getIrClassSymbol(firParentClass!!.symbol).owner
            } else {
                val lastName = nameSegments.last()
                firCandidates = firClass.declarations.filter { it is FirCallableDeclaration && it.symbol.name.asString() == lastName }
                parent = firParentClass?.let { classifierStorage.getIrClassSymbol(it.symbol).owner } ?: packageFragment
            }
        }

        // The next line should have singleOrNull, but in some cases we get multiple references to the same FIR declaration.
        val firDeclaration = firCandidates.firstOrNull { fir2IrComponents.signatureComposer.composeSignature(it) == signature }
            ?: return null

        return when (symbol) {
            is IrClassSymbol -> classifierStorage.getIrClassSymbol((firDeclaration as FirRegularClass).symbol).owner
            is IrConstructorSymbol -> {
                val firConstructor = firDeclaration as FirConstructor
                declarationStorage.getOrCreateIrConstructor(firConstructor, parent as IrClass)
            }
            is IrSimpleFunctionSymbol -> {
                val firSimpleFunction = firDeclaration as FirSimpleFunction
                declarationStorage.getOrCreateIrFunction(firSimpleFunction, parent)
            }
            is IrPropertySymbol -> {
                val firProperty = firDeclaration as FirProperty
                declarationStorage.getOrCreateIrProperty(firProperty, parent)
            }
            is IrFieldSymbol -> {
                val firField = firDeclaration as FirField
                declarationStorage.getOrCreateIrPropertyByPureField(firField, parent)
            }
            else -> error("Don't know how to deal with this symbol kind: $symbol")
        }
    }
}