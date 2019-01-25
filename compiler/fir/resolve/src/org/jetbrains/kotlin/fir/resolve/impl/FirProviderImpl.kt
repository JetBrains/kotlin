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

    override fun getFirClassifierBySymbol(symbol: ConeSymbol): FirNamedDeclaration? {
        return when (symbol) {
            is FirBasedSymbol<*> -> symbol.fir as? FirNamedDeclaration
            is ConeClassLikeSymbol -> getFirClassifierByFqName(symbol.classId)
            else -> error("!")
        }
    }

    override fun getSymbolByFqName(classId: ClassId): ConeSymbol? {
        return (getFirClassifierByFqName(classId) as? FirSymbolOwner<*>)?.symbol
    }

    override fun getCallableSymbols(callableId: CallableId): List<ConeSymbol> {
        return (callableMap[callableId] ?: emptyList()).filterIsInstance<FirSymbolOwner<*>>().map { it.symbol }
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

            override fun visitCallableMember(callableMember: FirCallableMember) {
                val callableId = when (containerFqName) {
                    FqName.ROOT -> CallableId(packageName, callableMember.name)
                    else -> CallableId(packageName, containerFqName, callableMember.name)
                }
                callableMap.merge(callableId, listOf(callableMember)) { a, b -> a + b }
            }

            override fun visitNamedFunction(namedFunction: FirNamedFunction) {
                visitCallableMember(namedFunction)
            }

            override fun visitProperty(property: FirProperty) {
                visitCallableMember(property)
            }
        })
    }

    private val fileMap = mutableMapOf<FqName, List<FirFile>>()
    private val classifierMap = mutableMapOf<ClassId, FirMemberDeclaration>()
    private val classifierContainerFileMap = mutableMapOf<ClassId, FirFile>()
    private val callableMap = mutableMapOf<CallableId, List<FirNamedDeclaration>>()

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> {
        return fileMap[fqName].orEmpty()
    }

    override fun getFirClassifierByFqName(fqName: ClassId): FirMemberDeclaration? {
        return classifierMap[fqName]
    }

}