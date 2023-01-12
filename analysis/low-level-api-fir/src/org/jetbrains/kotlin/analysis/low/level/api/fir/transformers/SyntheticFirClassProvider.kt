/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import java.util.WeakHashMap

internal interface SyntheticFirClassProvider {
    fun getFirClassifierContainerFileIfAny(classId: ClassId): FirFile?
    fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration?

    companion object {
        fun getInstance(session: FirSession): SyntheticFirClassProvider {
            return providersForThread.get()[session] ?: EmptySyntheticFirClassProvider
        }
    }
}

internal fun withSyntheticClasses(designation: FirDesignationWithFile, block: () -> Unit) {
    val firSession = designation.firFile.moduleData.session

    val compoundProvider = providersForThread.get()
        .getOrPut(firSession) { CompoundSyntheticFirClassProvider() }

    val provider = StaticSyntheticFirClassProvider.create(designation)

    try {
        compoundProvider.push(provider)
        block()
    } finally {
        compoundProvider.pop(provider)
    }
}

private class StaticSyntheticFirClassProvider private constructor(
    private val firFile: FirFile,
    private val classes: Map<ClassId, FirClassLikeDeclaration>
) : SyntheticFirClassProvider {
    override fun getFirClassifierContainerFileIfAny(classId: ClassId): FirFile? {
        return if (classId in classes) firFile else null
    }

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? {
        return classes[classId]
    }

    companion object {
        fun create(designation: FirDesignationWithFile): SyntheticFirClassProvider {
            val firFile = designation.firFile
            val firElement = designation.target

            val nodeInfoCollector = object : FirVisitorVoid() {
                val classes = mutableMapOf<ClassId, FirClassLikeDeclaration>()
                override fun visitElement(element: FirElement) {
                    if (element is FirClassLikeDeclaration) {
                        classes[element.symbol.classId] = element
                    }
                    element.acceptChildren(this)
                }
            }

            nodeInfoCollector.visitElement(firElement)
            return StaticSyntheticFirClassProvider(firFile, nodeInfoCollector.classes)
        }
    }
}

private class CompoundSyntheticFirClassProvider : SyntheticFirClassProvider {
    private val providers = ArrayDeque<SyntheticFirClassProvider>()

    fun push(provider: SyntheticFirClassProvider) {
        providers.addFirst(provider)
    }

    fun pop(provider: SyntheticFirClassProvider) {
        assert(providers.removeFirst() === provider)
    }

    override fun getFirClassifierContainerFileIfAny(classId: ClassId): FirFile? {
        return providers.asReversed()
            .firstNotNullOfOrNull { it.getFirClassifierContainerFileIfAny(classId) }
    }

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? {
        return providers.asReversed()
            .firstNotNullOfOrNull { it.getFirClassifierByFqName(classId) }
    }
}

private object EmptySyntheticFirClassProvider : SyntheticFirClassProvider {
    override fun getFirClassifierContainerFileIfAny(classId: ClassId) = null
    override fun getFirClassifierByFqName(classId: ClassId) = null
}

private val providersForThread: ThreadLocal<WeakHashMap<FirSession, CompoundSyntheticFirClassProvider>> =
    ThreadLocal.withInitial { WeakHashMap() }