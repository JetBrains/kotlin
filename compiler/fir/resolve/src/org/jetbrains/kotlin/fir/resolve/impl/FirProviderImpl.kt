/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.UnambiguousFqName
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

class FirProviderImpl(val session: FirSession) : FirProvider {

    fun recordFile(file: FirFile) {
        val packageName = file.packageFqName.toUnsafe()
        fileMap.merge(packageName, listOf(file)) { a, b -> a + b }

        file.acceptChildren(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {}

            var containerFqName: FqName = FqName.ROOT

            override fun visitClass(klass: FirClass) {
                val fqName = containerFqName.child(klass.name)
                classifierMap[UnambiguousFqName(packageName, fqName)] = klass

                containerFqName = fqName
                klass.acceptChildren(this)
                containerFqName = fqName.parent()
            }

            override fun visitTypeAlias(typeAlias: FirTypeAlias) {
                val fqName = containerFqName.child(typeAlias.name)
                classifierMap[UnambiguousFqName(packageName, fqName)] = typeAlias
            }
        })
    }

    private val fileMap = mutableMapOf<FqNameUnsafe, List<FirFile>>()
    private val classifierMap = mutableMapOf<UnambiguousFqName, FirMemberDeclaration>()

    override fun getFirFilesByPackage(fqName: FqNameUnsafe): List<FirFile> {


        return fileMap[fqName].orEmpty()
    }

    override fun getFirClassifierByFqName(fqName: UnambiguousFqName): FirMemberDeclaration? {
        return classifierMap[fqName]
    }

    override fun getFirTypeParameterByFqName(fqName: UnambiguousFqName, parameterName: Name): FirTypeParameter? {
        val typeParameterContainer = (getFirClassifierByFqName(fqName) as? FirTypeParameterContainer) ?: return null
        // TODO: Optimize search here
        return typeParameterContainer.typeParameters.find { it.name == parameterName }
    }
}