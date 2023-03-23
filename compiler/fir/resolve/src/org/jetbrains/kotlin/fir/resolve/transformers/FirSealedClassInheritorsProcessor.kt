/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.setSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping
import org.jetbrains.kotlin.name.ClassId

class FirSealedClassInheritorsProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirGlobalResolveProcessor(session, scopeSession, FirResolvePhase.SEALED_CLASS_INHERITORS) {
    override fun process(files: Collection<FirFile>) {
        val sealedClassInheritorsMap = mutableMapOf<FirRegularClass, MutableList<ClassId>>()
        val inheritorsCollector = InheritorsCollector(session)
        files.forEach {
            withFileAnalysisExceptionWrapping(it) {
                it.accept(inheritorsCollector, sealedClassInheritorsMap)
            }
        }
        files.forEach {
            withFileAnalysisExceptionWrapping(it) {
                it.transformSingle(InheritorsTransformer(sealedClassInheritorsMap), null)
            }
        }
    }

    class InheritorsCollector(val session: FirSession) : FirDefaultVisitor<Unit, MutableMap<FirRegularClass, MutableList<ClassId>>>() {
        override fun visitElement(element: FirElement, data: MutableMap<FirRegularClass, MutableList<ClassId>>) {}

        override fun visitFile(file: FirFile, data: MutableMap<FirRegularClass, MutableList<ClassId>>) {
            file.declarations.forEach { it.accept(this, data) }
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: MutableMap<FirRegularClass, MutableList<ClassId>>) {
            regularClass.declarations.forEach { it.accept(this, data) }

            if (regularClass.modality == Modality.SEALED) {
                data.computeIfAbsent(regularClass) { mutableListOf() }
            }

            val symbolProvider = session.symbolProvider

            for (typeRef in regularClass.superTypeRefs) {
                val parent = extractClassFromTypeRef(symbolProvider, typeRef).takeIf { it?.modality == Modality.SEALED } ?: continue
                // Inheritors of sealed class are allowed only in same package
                if (parent.classId.packageFqName != regularClass.classId.packageFqName) continue
                val inheritors = data.computeIfAbsent(parent) { mutableListOf() }
                inheritors += regularClass.symbol.classId
            }
        }

        private fun extractClassFromTypeRef(symbolProvider: FirSymbolProvider, typeRef: FirTypeRef): FirRegularClass? {
            val lookupTag = (typeRef.coneType as? ConeLookupTagBasedType)?.lookupTag ?: return null
            val classLikeSymbol: FirClassifierSymbol<*> = symbolProvider.getSymbolByLookupTag(lookupTag) ?: return null
            return when (classLikeSymbol) {
                is FirRegularClassSymbol -> classLikeSymbol.fir
                is FirTypeAliasSymbol -> {
                    classLikeSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
                    extractClassFromTypeRef(symbolProvider, classLikeSymbol.fir.expandedTypeRef)
                }
                else -> null
            }
        }
    }

    class InheritorsTransformer(private val inheritorsMap: MutableMap<FirRegularClass, MutableList<ClassId>>) : FirTransformer<Any?>() {
        override fun <E : FirElement> transformElement(element: E, data: Any?): E {
            return element
        }

        override fun transformFile(file: FirFile, data: Any?): FirFile {
            return withFileAnalysisExceptionWrapping(file) {
                file.transformChildren(this, data) as FirFile
            }
        }

        override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): FirStatement {
            if (regularClass.modality == Modality.SEALED) {
                val inheritors = inheritorsMap.remove(regularClass)
                if (inheritors != null) {
                    regularClass.setSealedClassInheritors(inheritors)
                }
            }
            if (inheritorsMap.isEmpty()) return regularClass
            return (regularClass.transformChildren(this, data) as FirRegularClass)
        }
    }

}
