/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class ContextCollectorVisitor(
    private val holder: SessionHolder,
    private val filter: (PsiElement) -> Response
) : FirDefaultVisitorVoid() {
    private val session: FirSession
        get() = holder.session

    private var isActive = true

    private val context = BodyResolveContext(
        returnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve,
        dataFlowAnalyzerContext = DataFlowAnalyzerContext(session)
    )

    private val result = HashMap<PsiElement, FirTowerDataContext>()

    operator fun get(element: PsiElement): FirTowerDataContext? {
        return result[element]
    }

    override fun visitElement(element: FirElement) {
        ProgressManager.checkCanceled()

        val psi = element.psi
        if (psi != null) {
            dumpContext(psi)
        }

        if (!isActive) {
            return
        }

        element.acceptChildren(this)
    }

    private fun dumpContext(psi: PsiElement?) {
        if (psi == null || psi in result) {
            return
        }

        val response = filter(psi)
        if (response != Response.SKIP) {
            result[psi] = context.towerDataContext
        }
        if (response == Response.STOP) {
            isActive = false
        }
    }

    override fun visitFile(file: FirFile) {
        file.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        context.withFile(file, holder) {
            super.visitFile(file)
        }
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
        context.forAnnotation {
            super.visitAnnotationCall(annotationCall)
        }
    }

    override fun visitRegularClass(regularClass: FirRegularClass) = withProcessor {
        regularClass.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        processSignatureAnnotations(regularClass)

        onActive {
            context.withContainingClass(regularClass) {
                processList(regularClass.typeParameters)

                onActive {
                    context.withRegularClass(regularClass, holder) {
                        val psiClassBody = (regularClass.psi as? KtClassOrObject)?.body
                        dumpContext(psiClassBody)

                        processChildren(regularClass)
                    }
                }
            }

            if (regularClass.isLocal) {
                context.storeClassIfNotNested(regularClass, session)
            }
        }
    }

    override fun visitConstructor(constructor: FirConstructor) {
        constructor.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        withProcessor {
            processSignatureAnnotations(constructor)

            onActive {
                context.withConstructor(constructor) {
                    val owningClass = context.containerIfAny as? FirRegularClass
                    context.forConstructorParameters(constructor, owningClass, holder) {
                        processList(constructor.valueParameters)
                    }

                    onActive {
                        context.forConstructorBody(constructor, session) {
                            processList(constructor.valueParameters)

                            onActive {
                                constructor.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                                process(constructor.body)
                            }
                        }
                    }

                    processChildren(constructor)
                }
            }
        }
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry) {
        context.forEnumEntry {
            super.visitEnumEntry(enumEntry)
        }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) = withProcessor {
        simpleFunction.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        processSignatureAnnotations(simpleFunction)

        onActive {
            context.withSimpleFunction(simpleFunction, session) {
                context.forFunctionBody(simpleFunction, holder) {
                    processList(simpleFunction.valueParameters)

                    onActive {
                        simpleFunction.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                        process(simpleFunction.body)
                    }
                }

                processChildren(simpleFunction)
            }
        }
    }

    override fun visitProperty(property: FirProperty) = withProcessor {
        property.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        processSignatureAnnotations(property)

        onActive {
            property.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            context.withProperty(property) {
                context.forPropertyInitializer {
                    process(property.initializer)
                    process(property.delegate)
                    process(property.backingField)
                }

                processChildren(property)
            }

            if (property.isLocal) {
                context.storeVariable(property, session)
            }
        }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
        // Signature annotations are already visited in 'visitProperty()'

        context.withPropertyAccessor(propertyAccessor.propertySymbol.fir, propertyAccessor, holder) {
            propertyAccessor.acceptChildren(this)
        }
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) = withProcessor {
        anonymousInitializer.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        processSignatureAnnotations(anonymousInitializer)

        onActive {
            anonymousInitializer.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            context.withAnonymousInitializer(anonymousInitializer, session) {
                processChildren(anonymousInitializer)
            }
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) = withProcessor {
        processSignatureAnnotations(anonymousFunction)

        onActive {
            context.withAnonymousFunction(anonymousFunction, holder, ResolutionMode.ContextIndependent) {
                context.forFunctionBody(anonymousFunction, holder) {
                    for (parameter in anonymousFunction.valueParameters) {
                        process(parameter)
                        context.storeVariable(parameter, holder.session)
                    }

                    process(anonymousFunction.body)
                }

                processChildren(anonymousFunction)
            }
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) = withProcessor {
        processSignatureAnnotations(anonymousObject)

        context.withAnonymousObject(anonymousObject, holder) {
            super.visitAnonymousObject(anonymousObject)
        }
    }

    override fun visitBlock(block: FirBlock) {
        context.forBlock(session) {
            super.visitBlock(block)

            val psiRightBrace = (block.psi as? KtBlockExpression)?.rBrace
            dumpContext(psiRightBrace)
        }
    }

    @ContextCollectorDsl
    private fun Processor.processSignatureAnnotations(declaration: FirDeclaration) {
        val visitor = object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element is FirDeclaration || element !is FirStatement) {
                    element.acceptChildren(this)
                }
            }

            override fun visitAnnotation(annotation: FirAnnotation) = process(annotation)
            override fun visitAnnotationCall(annotationCall: FirAnnotationCall) = process(annotationCall)
        }

        declaration.acceptChildren(visitor)
    }

    private inline fun withProcessor(block: Processor.() -> Unit) {
        Processor().block()
    }

    private inner class Processor {
        private val elementsToSkip = HashSet<FirElement>()

        @ContextCollectorDsl
        fun process(element: FirElement?) {
            onActive {
                if (element != null) {
                    element.accept(this@ContextCollectorVisitor)
                    elementsToSkip += element
                }
            }
        }

        @ContextCollectorDsl
        fun processList(elements: Collection<FirElement>) {
            for (element in elements) {
                onActive {
                    process(element)
                    elementsToSkip += element
                }
            }
        }

        @ContextCollectorDsl
        fun processChildren(element: FirElement) {
            onActive {
                val visitor = FilteringVisitor(this@ContextCollectorVisitor, elementsToSkip)
                element.acceptChildren(visitor)
            }
        }
    }

    private class FilteringVisitor(val delegate: FirVisitorVoid, val elementsToSkip: Set<FirElement>) : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element !in elementsToSkip) {
                element.accept(delegate)
            }
        }
    }

    private inline fun onActive(block: () -> Unit) {
        if (isActive) {
            block()
        }
    }

    enum class Response {
        CONTINUE,
        SKIP,
        STOP
    }
}

@DslMarker
private annotation class ContextCollectorDsl