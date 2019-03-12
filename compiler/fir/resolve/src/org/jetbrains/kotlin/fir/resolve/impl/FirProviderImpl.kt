/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FirProviderImpl(val session: FirSession) : FirProvider {
    override fun getFirCallableContainerFile(callableId: CallableId): FirFile? {
        return callableContainerMap[callableId]
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
        return (getFirClassifierByFqName(classId) as? FirSymbolOwner<*>)?.symbol as? ConeClassLikeSymbol
    }

    override fun getCallableSymbols(callableId: CallableId): List<ConeCallableSymbol> {
        return (callableMap[callableId] ?: emptyList())
            .filterIsInstance<FirSymbolOwner<*>>()
            .mapNotNull { it.symbol as? ConeCallableSymbol }
    }

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
                val callableId = (callableMemberDeclaration.symbol as ConeCallableSymbol).callableId
                callableMap.merge(callableId, listOf(callableMemberDeclaration)) { a, b -> a + b }
                callableContainerMap[callableId] = file
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
    private val callableMap = mutableMapOf<CallableId, List<FirNamedDeclaration>>()
    private val callableContainerMap = mutableMapOf<CallableId, FirFile>()

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> {
        return fileMap[fqName].orEmpty()
    }

    override fun getFirClassifierByFqName(fqName: ClassId): FirClassLikeDeclaration? {
        return classifierMap[fqName]
    }

}
