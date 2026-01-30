/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.utils.shouldUseCalleeReferenceAsItsSourceInIr
import org.jetbrains.kotlin.fir.backend.utils.startOffsetSkippingComments
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.evaluatedInitializer
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.unwrapOr
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*

interface EvaluatorHandlerTrait {
    val testServices: TestServices

    val globalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

}

class FirInterpreterDumpHandler(testServices: TestServices) : FirAnalysisHandler(testServices), EvaluatorHandlerTrait {
    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        info.partsForDependsOnModules.forEach {
            it.firFilesByTestFile.forEach { (testFile, firFile) ->
                processFile(testFile, firFile)
            }
        }
    }

    private fun processFile(testFile: TestFile, firFile: FirFile) {
        fun render(result: FirElement, start: Int, end: Int) {
            if (result !is FirLiteralExpression) return

            val message = result.value.toString()
            val metaInfo = ParsedCodeMetaInfo(
                start, end,
                attributes = mutableListOf(),
                tag = "EVALUATED",
                description = StringUtil.escapeLineBreak(message)
            )

            globalMetadataInfoHandler.addMetadataInfosForFile(testFile, listOf(metaInfo))
        }

        fun render(result: FirLiteralExpression, source: KtSourceElement?) {
            val start = source?.startOffset ?: return
            val end = result.source?.endOffset ?: return
            render(result, start, end)
        }

        data class Options(val renderLiterals: Boolean)

        class EvaluateAndRenderExpressions : FirVisitor<Unit, Options>() {
            // This set is used to avoid double rendering
            private val visitedElements = mutableSetOf<FirElement>()

            override fun visitElement(element: FirElement, data: Options) {
                element.acceptChildren(this, data)
            }

            override fun visitLiteralExpression(literalExpression: FirLiteralExpression, data: Options) {
                if (!data.renderLiterals) return
                render(literalExpression, literalExpression.source)
            }

            override fun visitProperty(property: FirProperty, data: Options) {
                if (property in visitedElements) return
                visitedElements.add(property)

                super.visitProperty(property, data)
                property.evaluatedInitializer?.unwrapOr<FirExpression> { }?.let { result ->
                    val (start, end) = property.initializer?.getCorrespondingIrOffset() ?: return
                    render(result, start, end)
                }
            }

            override fun visitValueParameter(valueParameter: FirValueParameter, data: Options) {
                if (valueParameter in visitedElements) return
                visitedElements.add(valueParameter)

                super.visitValueParameter(valueParameter, data)
                valueParameter.evaluatedInitializer?.unwrapOr<FirExpression> { }?.let { result ->
                    val (start, end) = valueParameter.defaultValue?.getCorrespondingIrOffset() ?: return
                    render(result, start, end)
                }
            }

            override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Options) {
                if (annotationCall in visitedElements) return
                visitedElements.add(annotationCall)

                super.visitAnnotationCall(annotationCall, data)
                annotationCall.argumentMapping.mapping.values.forEach { evaluated ->
                    evaluated.accept(this, data.copy(renderLiterals = true))
                }
            }

            override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Options) {
                // Visit annotations on type arguments
                resolvedTypeRef.delegatedTypeRef?.accept(this, data)
                return super.visitResolvedTypeRef(resolvedTypeRef, data)
            }

            private fun FirExpression.getCorrespondingIrOffset(): Pair<Int, Int>? {
                return if (this is FirQualifiedAccessExpression && this.shouldUseCalleeReferenceAsItsSourceInIr()) {
                    val calleeReference = this.calleeReference
                    val start = calleeReference.source?.startOffsetSkippingComments() ?: calleeReference.source?.startOffset ?: UNDEFINED_OFFSET
                    val end = this.source?.endOffset ?: return null
                    start to end
                } else {
                    val start = this.source?.startOffset ?: return null
                    val end = this.source?.endOffset ?: return null
                    start to end
                }
            }
        }

        firFile.accept(EvaluateAndRenderExpressions(), Options(renderLiterals = false))
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
