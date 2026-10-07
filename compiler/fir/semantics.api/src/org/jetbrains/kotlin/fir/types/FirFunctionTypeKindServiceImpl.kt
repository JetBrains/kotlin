/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKindExtractor
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.FirFunctionTypeKindExtension
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.functionTypeKindExtensions
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.ClassId

class FirFunctionTypeKindServiceImpl(private val session: FirSession) : FirFunctionTypeKindService() {
    private val nonReflectKindsFromExtensions = mutableListOf<FunctionTypeKind>()

    override val extractor: FunctionTypeKindExtractor = run {
        val kinds = buildList {
            add(FunctionTypeKind.Function)
            add(FunctionTypeKind.SuspendFunction)
            add(FunctionTypeKind.KFunction)
            add(FunctionTypeKind.KSuspendFunction)

            val registrar = object : FirFunctionTypeKindExtension.FunctionTypeKindRegistrar {
                override fun registerKind(nonReflectKind: FunctionTypeKind, reflectKind: FunctionTypeKind) {
                    require(nonReflectKind.reflectKind() == reflectKind)
                    require(reflectKind.nonReflectKind() == nonReflectKind)
                    add(nonReflectKind)
                    add(reflectKind)
                    nonReflectKindsFromExtensions += nonReflectKind
                }
            }

            for (extension in session.extensionService.functionTypeKindExtensions) {
                with(extension) { registrar.registerKinds() }
            }
        }.also { kinds ->
            val allNames = kinds.map { "${it.packageFqName}.${it.classNamePrefix}" }
            require(allNames.distinct() == allNames) {
                "There are clashing functional type kinds: $allNames"
            }
        }

        FunctionTypeKindExtractor(kinds)
    }

    override fun extractSingleSpecialKindForFunction(functionSymbol: FirFunctionSymbol<*>): FunctionTypeKind? {
        if (nonReflectKindsFromExtensions.isEmpty()) {
            return FunctionTypeKind.SuspendFunction.takeIf { functionSymbol.isSuspend }
        }

        return extractAllSpecialKindsForFunction(functionSymbol).singleOrNull()
    }

    override fun extractAllSpecialKindsForFunction(functionSymbol: FirFunctionSymbol<*>): List<FunctionTypeKind> {
        return extractSpecialKindsImpl(
            functionSymbol,
            { isSuspend },
            {
                when (functionSymbol) {
                    is FirAnonymousFunctionSymbol -> functionSymbol.annotations.mapNotNull { it.toAnnotationClassId(session) }
                    else -> resolvedAnnotationClassIds
                }
            }
        )
    }

    override fun extractAllSpecialKindsForFunctionTypeRef(typeRef: FirFunctionTypeRef): List<FunctionTypeKind> {
        return extractSpecialKindsImpl(typeRef, { isSuspend }, { annotations.mapNotNull { it.toAnnotationClassId(session) } })
    }

    override fun extractSingleExtensionKindForDeserializedConeType(
        classId: ClassId,
        annotations: List<FirAnnotation>
    ): FunctionTypeKind? {
        if (nonReflectKindsFromExtensions.isEmpty() || annotations.isEmpty()) return null
        val baseKind = extractor.getFunctionalClassKind(classId.packageFqName, classId.shortClassName.asString()) ?: return null
        if (baseKind.nonReflectKind() != FunctionTypeKind.Function) return null
        val matchingExtensionKinds = buildList {
            extractKindsFromAnnotations(annotations.mapNotNull { it.toAnnotationClassId(session) })
        }
        val matchingKind = matchingExtensionKinds.singleOrNull() ?: return null
        return when (baseKind.isReflectType) {
            false -> matchingKind
            true -> matchingKind.reflectKind()
        }
    }

    private inline fun <T> extractSpecialKindsImpl(
        source: T,
        isSuspend: T.() -> Boolean,
        annotations: T.() -> List<ClassId>
    ): List<FunctionTypeKind> {
        return buildList {
            if (source.isSuspend()) {
                add(FunctionTypeKind.SuspendFunction)
            }
            if (nonReflectKindsFromExtensions.isNotEmpty()) {
                extractKindsFromAnnotations(source.annotations())
            }
        }
    }

    private fun MutableList<FunctionTypeKind>.extractKindsFromAnnotations(annotations: List<ClassId>) {
        for (annotationClassId in annotations) {
            for (kind in nonReflectKindsFromExtensions) {
                if (kind.annotationOnInvokeClassId == annotationClassId) {
                    add(kind)
                }
            }
        }
    }
}
