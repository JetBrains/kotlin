/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.directInheritors
import org.jetbrains.kotlin.fir.declarations.setDirectInheritors
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassIdFromDependencies
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeErrorLookupTag
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.lookupTagIfAny
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping
import org.jetbrains.kotlin.name.ClassId

class FirDirectClassInheritorsResolver(
    override val session: FirSession
) : SessionHolder, FirSessionComponent {
    private val inheritorsMap = mutableMapOf<FirRegularClass, MutableSet<ClassId>>()
    private val inheritorsCollector = DirectClassInheritorsCollector(session)
    private val inheritorsTransformer = DirectClassInheritorsTransformer()

    fun resolveInheritors(file: FirFile): Unit = file.accept(inheritorsCollector, inheritorsMap)

    fun storeInheritors(file: FirFile) {
        file.transformSingle(inheritorsTransformer, inheritorsMap)
    }
}

private class DirectClassInheritorsCollector(
    override val session: FirSession
) : SessionHolder, FirDefaultVisitor<Unit, MutableMap<FirRegularClass, MutableSet<ClassId>>>() {
    override fun visitElement(element: FirElement, data: MutableMap<FirRegularClass, MutableSet<ClassId>>): Unit = Unit

    override fun visitFile(file: FirFile, data: MutableMap<FirRegularClass, MutableSet<ClassId>>): Unit = file.acceptChildren(this, data)

    override fun visitRegularClass(regularClass: FirRegularClass, data: MutableMap<FirRegularClass, MutableSet<ClassId>>) {
        regularClass.acceptChildren(this, data)

        for (typeRef in regularClass.superTypeRefs) {
            val parent = extractClassFromTypeRef(typeRef) ?: continue
            val inheritors = data.computeIfAbsent(parent) { mutableSetOf() }
            inheritors += regularClass.symbol.classId
        }

        collectInheritorsOfCorrespondingExpectSealedClass(regularClass.classId, data.computeIfAbsent(regularClass) { mutableSetOf() })
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: MutableMap<FirRegularClass, MutableSet<ClassId>>) {
        if (!typeAlias.isActual) return
        val expansionClass = typeAlias.expandedTypeRef.coneType.toRegularClassSymbol(session)?.fir ?: return
        collectInheritorsOfCorrespondingExpectSealedClass(typeAlias.classId, data.computeIfAbsent(expansionClass) { mutableSetOf() })
    }

    private fun collectInheritorsOfCorrespondingExpectSealedClass(expectClassId: ClassId, inheritors: MutableSet<ClassId>) {
        if (LanguageFeature.MultiPlatformProjects.isDisabled()) return
        val correspondingExpectClass = session.getRegularClassSymbolByClassIdFromDependencies(expectClassId)?.fir ?: return
        if (correspondingExpectClass.isExpect) {
            inheritors.addAll(correspondingExpectClass.directInheritors)
        }
    }

    private fun extractClassFromTypeRef(typeRef: FirTypeRef): FirRegularClass? {
        val lookupTag = typeRef.coneType.lookupTagIfAny ?: return null
        if (lookupTag is ConeClassLikeErrorLookupTag) {
            println("${typeRef.coneType}")
        }
        val classLikeSymbol: FirClassifierSymbol<*> = lookupTag.toSymbol(session) ?: return null
        return when (classLikeSymbol) {
            is FirRegularClassSymbol -> classLikeSymbol.fir
            is FirTypeAliasSymbol -> {
                classLikeSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
                extractClassFromTypeRef(classLikeSymbol.fir.expandedTypeRef)
            }
            else -> null
        }
    }
}

private class DirectClassInheritorsTransformer : FirTransformer<MutableMap<FirRegularClass, MutableSet<ClassId>>>() {
    override fun <E : FirElement> transformElement(element: E, data: MutableMap<FirRegularClass, MutableSet<ClassId>>): E {
        return element
    }

    override fun transformFile(file: FirFile, data: MutableMap<FirRegularClass, MutableSet<ClassId>>): FirFile {
        return withFileAnalysisExceptionWrapping(file) {
            file.transformChildren(this, data) as FirFile
        }
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: MutableMap<FirRegularClass, MutableSet<ClassId>>
    ): FirStatement {
        if (data.isEmpty()) return regularClass
        val inheritors = data.remove(regularClass)
        if (inheritors != null) {
            regularClass.setDirectInheritors(inheritors)
        }
        return (regularClass.transformChildren(this, data) as FirRegularClass)
    }
}
