/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirWholeFileResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector.ContextKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector.Context
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector.FilterResponse
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

@LLFirInternals
object ContextCollector {
    enum class ContextKind {
        /** Represents the context of the declaration itself. */
        SELF,

        /** Represents the context inside the body (of a class, function, block, etc.) */
        BODY
    }

    class Context(
        val towerDataContext: FirTowerDataContext,
        val smartCasts: Map<FirBasedSymbol<*>, Set<ConeKotlinType>>,
    )

    enum class FilterResponse {
        /** Store context for the element and continue the traversal. */
        CONTINUE,

        /** Store context for the element and stop traversal. */
        STOP,

        /** Skip the element and continue the traversal. */
        SKIP
    }

    /**
     * Process the [file], collecting contexts for the [targetElement] and all its PSI tree parents.
     */
    fun process(file: FirFile, holder: SessionHolder, targetElement: PsiElement, preferBody: Boolean): Context? {
        val acceptedElements = targetElement.parentsWithSelf.toSet()

        val contextProvider = process(computeResolveTarget(file, targetElement), holder) { candidate ->
            when (candidate) {
                targetElement -> FilterResponse.STOP
                in acceptedElements -> FilterResponse.CONTINUE
                else -> FilterResponse.SKIP
            }
        }

        if (preferBody) {
            val bodyContext = contextProvider[targetElement, ContextKind.BODY]
            if (bodyContext != null) {
                return bodyContext
            }
        }

        return acceptedElements.firstNotNullOfOrNull { contextProvider[it, ContextKind.SELF] }
    }

    private fun computeResolveTarget(file: FirFile, targetElement: PsiElement): LLFirResolveTarget {
        val contextKtDeclaration = targetElement.getNonLocalContainingOrThisDeclaration()
        if (contextKtDeclaration != null) {
            val designationPath = FirElementFinder.collectDesignationPath(file, contextKtDeclaration)
            if (designationPath != null) {
                return FirDesignationWithFile(designationPath.path, designationPath.target, file).asResolveTarget()
            }
        }

        return LLFirWholeFileResolveTarget(file)
    }

    /**
     * Processes the [FirFile] that owns the [target], collecting contexts for elements matching the [filter].
     */
    fun process(target: LLFirResolveTarget, holder: SessionHolder, filter: (PsiElement) -> FilterResponse): ContextProvider {
        val pathIterator = target.path.iterator()

        val visitor = ContextCollectorVisitor(holder, filter) {
            if (pathIterator.hasNext()) pathIterator.next() else null
        }

        target.firFile.accept(visitor)
        visitor.processTarget(target)

        return ContextProvider { element, kind -> visitor[element, kind] }
    }

    fun interface ContextProvider {
        operator fun get(element: PsiElement, kind: ContextKind): Context?
    }
}

private class ContextCollectorVisitor(
    private val holder: SessionHolder,
    private val filter: (PsiElement) -> FilterResponse,
    private val interceptor: () -> FirElement?
) : FirDefaultVisitorVoid() {
    private data class ContextKey(val element: PsiElement, val kind: ContextKind)

    operator fun get(element: PsiElement, kind: ContextKind): Context? {
        val key = ContextKey(element, kind)
        return result[key]
    }

    private val session: FirSession
        get() = holder.session

    private var isActive = true

    private val context = BodyResolveContext(
        returnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve,
        dataFlowAnalyzerContext = DataFlowAnalyzerContext(session)
    )

    private var smartCasts: PersistentMap<FirBasedSymbol<*>, Set<ConeKotlinType>> = persistentHashMapOf()

    private val result = HashMap<ContextKey, Context>()

    fun processTarget(target: LLFirResolveTarget) {
        target.firFile.accept(this)
    }

    override fun visitElement(element: FirElement) {
        dumpContext(element.psi, ContextKind.SELF)

        onActive {
            element.acceptChildren(this)
        }
    }

    private fun dumpContext(psi: PsiElement?, kind: ContextKind) {
        ProgressManager.checkCanceled()

        if (psi == null) {
            return
        }

        val key = ContextKey(psi, kind)
        if (key in result) {
            return
        }

        val response = filter(psi)
        if (response != FilterResponse.SKIP) {
            result[key] = Context(context.towerDataContext, smartCasts)
        }

        if (response == FilterResponse.STOP) {
            isActive = false
        }
    }

    override fun visitFile(file: FirFile) {
        file.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        context.withFile(file, holder) {
            withInterceptor {
                super.visitFile(file)
            }
        }
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
        context.forAnnotation {
            super.visitAnnotationCall(annotationCall)
        }
    }

    override fun visitRegularClass(regularClass: FirRegularClass) = withProcessor {
        dumpContext(regularClass.psi, ContextKind.SELF)

        regularClass.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        processSignatureAnnotations(regularClass)

        context.withContainingClass(regularClass) {
            processList(regularClass.typeParameters)

            context.withRegularClass(regularClass, holder) {
                dumpContext(regularClass.psi, ContextKind.BODY)

                onActive {
                    withInterceptor {
                        processChildren(regularClass)
                    }
                }
            }
        }

        if (regularClass.isLocal) {
            context.storeClassIfNotNested(regularClass, session)
        }
    }

    override fun visitConstructor(constructor: FirConstructor) = withProcessor {
        dumpContext(constructor.psi, ContextKind.SELF)

        constructor.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        processSignatureAnnotations(constructor)

        context.withConstructor(constructor) {
            val owningClass = context.containerIfAny as? FirRegularClass
            context.forConstructorParameters(constructor, owningClass, holder) {
                processList(constructor.valueParameters)
            }

            context.forConstructorBody(constructor, session) {
                processList(constructor.valueParameters)

                dumpContext(constructor.psi, ContextKind.BODY)

                onActive {
                    constructor.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                    process(constructor.body)
                }
            }

            onActive {
                context.forDelegatedConstructorCall(constructor, owningClass = null, holder) {
                    process(constructor.delegatedConstructor)
                }
            }

            onActive {
                processChildren(constructor)
            }
        }
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry) {
        enumEntry.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        context.forEnumEntry {
            super.visitEnumEntry(enumEntry)
        }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) = withProcessor {
        dumpContext(simpleFunction.psi, ContextKind.SELF)

        simpleFunction.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        processSignatureAnnotations(simpleFunction)

        context.withSimpleFunction(simpleFunction, session) {
            context.forFunctionBody(simpleFunction, holder) {
                processList(simpleFunction.valueParameters)

                dumpContext(simpleFunction.psi, ContextKind.BODY)

                onActive {
                    simpleFunction.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                    process(simpleFunction.body)
                }
            }

            onActive {
                processChildren(simpleFunction)
            }
        }
    }

    override fun visitProperty(property: FirProperty) = withProcessor {
        dumpContext(property.psi, ContextKind.SELF)

        property.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        processSignatureAnnotations(property)

        onActive {
            property.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            context.withProperty(property) {
                context.forPropertyInitializer {
                    onActive {
                        process(property.initializer)
                    }

                    onActive {
                        process(property.delegate)
                    }

                    onActive {
                        process(property.backingField)
                    }
                }

                onActive {
                    processChildren(property)
                }
            }
        }

        if (property.isLocal) {
            context.storeVariable(property, session)
        }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) = withProcessor {
        dumpContext(propertyAccessor.psi, ContextKind.SELF)

        processSignatureAnnotations(propertyAccessor)

        context.withPropertyAccessor(propertyAccessor.propertySymbol.fir, propertyAccessor, holder) {
            dumpContext(propertyAccessor.psi, ContextKind.BODY)

            onActive {
                processChildren(propertyAccessor)
            }
        }
    }

    override fun visitValueParameter(valueParameter: FirValueParameter) = withProcessor {
        dumpContext(valueParameter.psi, ContextKind.SELF)

        processSignatureAnnotations(valueParameter)

        context.withValueParameter(valueParameter, session) {
            dumpContext(valueParameter.psi, ContextKind.SELF)

            onActive {
                processChildren(valueParameter)
            }
        }
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) = withProcessor {
        dumpContext(anonymousInitializer.psi, ContextKind.SELF)

        anonymousInitializer.lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        processSignatureAnnotations(anonymousInitializer)

        context.withAnonymousInitializer(anonymousInitializer, session) {
            dumpContext(anonymousInitializer.psi, ContextKind.BODY)

            onActive {
                anonymousInitializer.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                processChildren(anonymousInitializer)
            }
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) = withProcessor {
        dumpContext(anonymousFunction.psi, ContextKind.SELF)

        processSignatureAnnotations(anonymousFunction)

        context.withAnonymousFunction(anonymousFunction, holder, ResolutionMode.ContextIndependent) {
            for (parameter in anonymousFunction.valueParameters) {
                process(parameter)
                context.storeVariable(parameter, holder.session)
            }

            dumpContext(anonymousFunction.psi, ContextKind.BODY)

            onActive {
                process(anonymousFunction.body)
            }

            onActive {
                processChildren(anonymousFunction)
            }
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) = withProcessor {
        dumpContext(anonymousObject.psi, ContextKind.SELF)

        processSignatureAnnotations(anonymousObject)

        context.withAnonymousObject(anonymousObject, holder) {
            dumpContext(anonymousObject.psi, ContextKind.BODY)

            onActive {
                processChildren(anonymousObject)
            }
        }
    }

    override fun visitBlock(block: FirBlock) = withProcessor {
        dumpContext(block.psi, ContextKind.SELF)

        context.forBlock(session) {
            processChildren(block)

            dumpContext(block.psi, ContextKind.BODY)
        }
    }

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression) {
        if (smartCastExpression.isStable) {
            val symbol = smartCastExpression.originalExpression.toResolvedCallableSymbol()
            if (symbol != null) {
                val previousSmartCasts = smartCasts
                try {
                    smartCasts = smartCasts.put(symbol, smartCastExpression.typesFromSmartCast.toSet())
                    super.visitSmartCastExpression(smartCastExpression)
                } finally {
                    smartCasts = previousSmartCasts
                }
            }
        }

        super.visitSmartCastExpression(smartCastExpression)
    }

    @ContextCollectorDsl
    private fun Processor.processSignatureAnnotations(declaration: FirDeclaration) {
        for (annotation in declaration.annotations) {
            onActive {
                process(annotation)
            }
        }
    }

    private inline fun withProcessor(block: Processor.() -> Unit) {
        Processor(this).block()
    }

    private class Processor(private val delegate: FirVisitorVoid) {
        private val elementsToSkip = HashSet<FirElement>()

        @ContextCollectorDsl
        fun process(element: FirElement?) {
            if (element != null) {
                element.accept(delegate)
                elementsToSkip += element
            }
        }

        @ContextCollectorDsl
        fun processList(elements: Collection<FirElement>) {
            for (element in elements) {
                process(element)
                elementsToSkip += element
            }
        }

        @ContextCollectorDsl
        fun processChildren(element: FirElement) {
            val visitor = FilteringVisitor(delegate, elementsToSkip)
            element.acceptChildren(visitor)
        }
    }

    private class FilteringVisitor(val delegate: FirVisitorVoid, val elementsToSkip: Set<FirElement>) : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element !in elementsToSkip) {
                element.accept(delegate)
            }
        }
    }

    private inline fun withInterceptor(block: () -> Unit) {
        val target = interceptor()
        if (target != null) {
            target.accept(this)
        } else {
            block()
        }
    }

    private inline fun onActive(block: () -> Unit) {
        if (isActive) {
            block()
        }
    }
}

@DslMarker
private annotation class ContextCollectorDsl