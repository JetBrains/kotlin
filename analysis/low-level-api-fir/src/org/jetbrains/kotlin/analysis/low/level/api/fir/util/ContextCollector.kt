/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
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

internal object ContextCollector {
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
     * Get the most precise context available for the [targetElement] in the [file].
     *
     * @param file The file to process.
     * @param holder The [SessionHolder] for the session that owns a [file].
     * @param targetElement The most precise element for which the context is required.
     * @param bodyElement An element for which the [ContextKind.BODY] context is preferred.
     *
     * Returns the context of the [targetElement] if available, or of one of its tree parents.
     * Returns `null` if the context was not collected.
     */
    fun process(file: FirFile, holder: SessionHolder, targetElement: PsiElement, bodyElement: PsiElement? = targetElement): Context? {
        val isBodyContextCollected = bodyElement != null
        val acceptedElements = targetElement.parentsWithSelf.toSet()

        val contextProvider = process(file, computeDesignation(file, targetElement), holder, isBodyContextCollected) { candidate ->
            when (candidate) {
                targetElement -> FilterResponse.STOP
                in acceptedElements -> FilterResponse.CONTINUE
                else -> FilterResponse.SKIP
            }
        }

        for (acceptedElement in acceptedElements) {
            if (acceptedElement === bodyElement) {
                val bodyContext = contextProvider[acceptedElement, ContextKind.BODY]
                if (bodyContext != null) {
                    return bodyContext
                }
            }

            val elementContext = contextProvider[acceptedElement, ContextKind.SELF]
            if (elementContext != null) {
                return elementContext
            }
        }

        return null
    }

    private fun computeDesignation(file: FirFile, targetElement: PsiElement): FirDesignation? {
        val contextKtDeclaration = targetElement.getNonLocalContainingOrThisDeclaration()
        if (contextKtDeclaration != null) {
            val designationPath = FirElementFinder.collectDesignationPath(file, contextKtDeclaration)
            if (designationPath != null) {
                return FirDesignation(designationPath.path, designationPath.target)
            }
        }

        return null
    }

    /**
     * Processes the [FirFile], collecting contexts for elements matching the [filter].
     *
     * @param file The file to process.
     * @param designation The declaration to process. If `null`, all declarations in the [file] are processed.
     * @param holder The [SessionHolder] for the session that owns a [file].
     * @param shouldCollectBodyContext If `true`, [ContextKind.BODY] is collected where available.
     * @param filter The filter predicate. Context is collected only for [PsiElement]s for which the [filter] returns `true`.
     */
    fun process(
        file: FirFile,
        designation: FirDesignation?,
        holder: SessionHolder,
        shouldCollectBodyContext: Boolean,
        filter: (PsiElement) -> FilterResponse
    ): ContextProvider {
        val interceptor = designation?.let(::DesignationInterceptor) ?: { null }
        val visitor = ContextCollectorVisitor(holder, shouldCollectBodyContext, filter, interceptor)
        file.accept(visitor)

        return ContextProvider { element, kind -> visitor[element, kind] }
    }

    private class DesignationInterceptor(private val designation: FirDesignation) : () -> FirElement? {
        private val targetIterator = iterator {
            yieldAll(designation.path)
            yield(designation.target)
        }

        override fun invoke(): FirElement? {
            return if (targetIterator.hasNext()) targetIterator.next() else null
        }
    }

    fun interface ContextProvider {
        operator fun get(element: PsiElement, kind: ContextKind): Context?
    }
}

private class ContextCollectorVisitor(
    private val holder: SessionHolder,
    private val shouldCollectBodyContext: Boolean,
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
        returnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
        dataFlowAnalyzerContext = DataFlowAnalyzerContext(session)
    )

    private var smartCasts: PersistentMap<FirBasedSymbol<*>, Set<ConeKotlinType>> = persistentMapOf()

    private val result = HashMap<ContextKey, Context>()

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

        if (kind == ContextKind.BODY && !shouldCollectBodyContext) {
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
        context.withFile(file, holder) {
            withInterceptor {
                super.visitFile(file)
            }
        }
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
        dumpContext(annotationCall.psi, ContextKind.SELF)

        onActiveBody {
            context.forAnnotation {
                dumpContext(annotationCall.psi, ContextKind.BODY)

                // Technically, annotation arguments might contain arbitrary expressions.
                // However, such cases are very rare, as it's currently forbidden in Kotlin.
                // Here we ignore declarations that might be inside such expressions, avoiding unnecessary tree traversal.
            }
        }
    }

    override fun visitRegularClass(regularClass: FirRegularClass) = withProcessor {
        dumpContext(regularClass.psi, ContextKind.SELF)

        processSignatureAnnotations(regularClass)

        onActiveBody {
            regularClass.lazyResolveToPhase(FirResolvePhase.STATUS)

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
        }

        if (regularClass.isLocal) {
            context.storeClassIfNotNested(regularClass, session)
        }
    }

    override fun visitConstructor(constructor: FirConstructor) = withProcessor {
        dumpContext(constructor.psi, ContextKind.SELF)

        processSignatureAnnotations(constructor)

        onActiveBody {
            constructor.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            context.withConstructor(constructor) {
                val owningClass = context.containerIfAny as? FirRegularClass
                context.forConstructorParameters(constructor, owningClass, holder) {
                    processList(constructor.valueParameters)
                }

                context.forConstructorBody(constructor, session) {
                    processList(constructor.valueParameters)

                    dumpContext(constructor.psi, ContextKind.BODY)

                    onActive {
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
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry) {
        dumpContext(enumEntry.psi, ContextKind.SELF)

        onActiveBody {
            enumEntry.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            context.forEnumEntry {
                dumpContext(enumEntry.psi, ContextKind.BODY)

                onActive {
                    super.visitEnumEntry(enumEntry)
                }
            }
        }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) = withProcessor {
        dumpContext(simpleFunction.psi, ContextKind.SELF)

        processSignatureAnnotations(simpleFunction)

        onActiveBody {
            simpleFunction.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            context.withSimpleFunction(simpleFunction, session) {
                context.forFunctionBody(simpleFunction, holder) {
                    processList(simpleFunction.valueParameters)

                    dumpContext(simpleFunction.psi, ContextKind.BODY)

                    onActive {
                        process(simpleFunction.body)
                    }
                }

                onActive {
                    processChildren(simpleFunction)
                }
            }
        }
    }

    override fun visitProperty(property: FirProperty) = withProcessor {
        dumpContext(property.psi, ContextKind.SELF)

        processSignatureAnnotations(property)

        onActiveBody {
            property.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            context.withProperty(property) {
                dumpContext(property.psi, ContextKind.BODY)

                onActive {
                    context.forPropertyInitializer {
                        process(property.initializer)

                        onActive {
                            process(property.delegate)
                        }

                        onActive {
                            process(property.backingField)
                        }
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

        onActiveBody {
            context.withPropertyAccessor(propertyAccessor.propertySymbol.fir, propertyAccessor, holder) {
                dumpContext(propertyAccessor.psi, ContextKind.BODY)

                onActive {
                    processChildren(propertyAccessor)
                }
            }
        }
    }

    override fun visitValueParameter(valueParameter: FirValueParameter) = withProcessor {
        dumpContext(valueParameter.psi, ContextKind.SELF)

        processSignatureAnnotations(valueParameter)

        onActiveBody {
            context.withValueParameter(valueParameter, session) {
                dumpContext(valueParameter.psi, ContextKind.BODY)

                onActive {
                    processChildren(valueParameter)
                }
            }
        }

    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) = withProcessor {
        dumpContext(anonymousInitializer.psi, ContextKind.SELF)

        processSignatureAnnotations(anonymousInitializer)

        onActiveBody {
            context.withAnonymousInitializer(anonymousInitializer, session) {
                dumpContext(anonymousInitializer.psi, ContextKind.BODY)

                onActive {
                    anonymousInitializer.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                    processChildren(anonymousInitializer)
                }
            }
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) = withProcessor {
        dumpContext(anonymousFunction.psi, ContextKind.SELF)

        processSignatureAnnotations(anonymousFunction)

        onActiveBody {
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

    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) = withProcessor {
        dumpContext(anonymousObject.psi, ContextKind.SELF)

        processSignatureAnnotations(anonymousObject)

        onActiveBody {
            context.withAnonymousObject(anonymousObject, holder) {
                dumpContext(anonymousObject.psi, ContextKind.BODY)

                onActive {
                    processChildren(anonymousObject)
                }
            }
        }

    }

    override fun visitBlock(block: FirBlock) = withProcessor {
        dumpContext(block.psi, ContextKind.SELF)

        onActiveBody {
            context.forBlock(session) {
                processChildren(block)

                dumpContext(block.psi, ContextKind.BODY)
            }
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
                    return
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

    private inline fun onActiveBody(block: () -> Unit) {
        if (isActive || shouldCollectBodyContext) {
            block()
        }
    }
}

@DslMarker
private annotation class ContextCollectorDsl