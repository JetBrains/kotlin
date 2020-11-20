/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.ClassId

class FirSealedClassInheritorsProcessor(session: FirSession, scopeSession: ScopeSession) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer = FirSealedClassInheritorsTransformer()
}

class FirSealedClassInheritorsTransformer : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        throw IllegalStateException("Should not be there")
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        val sealedClassInheritorsMap = mutableMapOf<FirRegularClass, MutableList<ClassId>>()
        file.accept(InheritorsCollector, sealedClassInheritorsMap)
        if (sealedClassInheritorsMap.isEmpty()) return file.compose()
        return file.transform(InheritorsTransformer(sealedClassInheritorsMap), null)
    }

    private class InheritorsTransformer(private val inheritorsMap: MutableMap<FirRegularClass, MutableList<ClassId>>) : FirTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            return element.compose()
        }

        override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirDeclaration> {
            return (file.transformChildren(this, data) as FirFile).compose()
        }

        override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirStatement> {
            if (regularClass.modality == Modality.SEALED) {
                val inheritors = inheritorsMap.remove(regularClass)
                if (inheritors != null) {
                    regularClass.sealedInheritors = inheritors
                }
            }
            if (inheritorsMap.isEmpty()) return regularClass.compose()
            return (regularClass.transformChildren(this, data) as FirRegularClass).compose()
        }
    }

    private object InheritorsCollector : FirDefaultVisitor<Unit, MutableMap<FirRegularClass, MutableList<ClassId>>>() {
        override fun visitElement(element: FirElement, data: MutableMap<FirRegularClass, MutableList<ClassId>>) {}

        override fun visitFile(file: FirFile, data: MutableMap<FirRegularClass, MutableList<ClassId>>) {
            file.declarations.forEach { it.accept(this, data) }
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: MutableMap<FirRegularClass, MutableList<ClassId>>) {
            regularClass.declarations.forEach { it.accept(this, data) }

            if (regularClass.modality == Modality.SEALED) {
                data.computeIfAbsent(regularClass) { mutableListOf() }
            }

            val symbolProvider = regularClass.session.firSymbolProvider

            for (typeRef in regularClass.superTypeRefs) {
                val parent = extractClassFromTypeRef(symbolProvider, typeRef).takeIf { it?.modality == Modality.SEALED } ?: continue
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
                    classLikeSymbol.ensureResolved(FirResolvePhase.SUPER_TYPES, symbolProvider.session)
                    extractClassFromTypeRef(symbolProvider, classLikeSymbol.fir.expandedTypeRef)
                }
                else -> null
            }
        }
    }
}

object SealedClassInheritorsKey : FirDeclarationDataKey()

var FirRegularClass.sealedInheritors: List<ClassId>? by FirDeclarationDataRegistry.data(SealedClassInheritorsKey)
