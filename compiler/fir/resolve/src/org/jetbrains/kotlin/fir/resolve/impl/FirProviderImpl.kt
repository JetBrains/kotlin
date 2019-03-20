/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.FirSymbolOwner
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirProviderImpl(val session: FirSession) : FirProvider {
    override fun getFirCallableContainerFile(symbol: ConeCallableSymbol): FirFile? {
        return callableContainerMap[symbol]
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
        return (getFirClassifierByFqName(classId) as? FirSymbolOwner<*>)?.symbol as? ConeClassLikeSymbol
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<ConeCallableSymbol> {
        return (callableMap[CallableId(packageFqName, null, name)] ?: emptyList())
    }

    override fun getClassDeclaredMemberScope(classId: ClassId) =
        (getFirClassifierByFqName(classId) as? FirRegularClass)?.let(::FirClassDeclaredMemberScope)

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        return classifierContainerFileMap[fqName] ?: error("Couldn't find container for $fqName")
    }

    fun recordFile(file: FirFile) {
        val packageName = file.packageFqName
        fileMap.merge(packageName, listOf(file)) { a, b -> a + b }

        file.acceptChildren(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {}

            var containerFqName: FqName = FqName.ROOT

            override fun visitRegularClass(regularClass: FirRegularClass) {
                val fqName = containerFqName.child(regularClass.name)
                val classId = ClassId(packageName, fqName, false)
                classifierMap[classId] = regularClass
                classifierContainerFileMap[classId] = file

                containerFqName = fqName
                regularClass.acceptChildren(this)
                containerFqName = fqName.parent()
            }

            override fun visitTypeAlias(typeAlias: FirTypeAlias) {
                val fqName = containerFqName.child(typeAlias.name)
                val classId = ClassId(packageName, fqName, false)
                classifierMap[classId] = typeAlias
                classifierContainerFileMap[classId] = file
            }

            override fun visitCallableMemberDeclaration(callableMemberDeclaration: FirCallableMemberDeclaration) {
                val symbol = callableMemberDeclaration.symbol as ConeCallableSymbol
                val callableId = symbol.callableId
                callableMap.merge(callableId, listOf(symbol)) { a, b -> a + b }
                callableContainerMap[symbol] = file
            }

            override fun visitConstructor(constructor: FirConstructor) {
                visitCallableMemberDeclaration(constructor)
            }

            override fun visitNamedFunction(namedFunction: FirNamedFunction) {
                visitCallableMemberDeclaration(namedFunction)
            }

            override fun visitProperty(property: FirProperty) {
                visitCallableMemberDeclaration(property)
            }
        })
    }

    private val fileMap = mutableMapOf<FqName, List<FirFile>>()
    private val classifierMap = mutableMapOf<ClassId, FirClassLikeDeclaration>()
    private val classifierContainerFileMap = mutableMapOf<ClassId, FirFile>()
    private val callableMap = mutableMapOf<CallableId, List<ConeCallableSymbol>>()
    private val callableContainerMap = mutableMapOf<ConeCallableSymbol, FirFile>()

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> {
        return fileMap[fqName].orEmpty()
    }

    override fun getFirClassifierByFqName(fqName: ClassId): FirClassLikeDeclaration? {
        return classifierMap[fqName]
    }

}
