/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.withFirDesignationEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.isAutonomousElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector.Context
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector.ContextKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector.FilterResponse
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.memberDeclarationNameOrNull
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitValue
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ClassExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.smartCastedType
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.addReceiversFromExtensions
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.types.SmartcastStability
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

object ContextCollector {
    enum class ContextKind {
        /** Represents the context of the declaration itself. */
        SELF,

        /** Represents the context inside the body (of a class, function, block, etc.) */
        BODY
    }

    /**
     * Represents resolution context of a specific place in code (a context).
     *
     * @param towerDataContext a list of tower data elements that may define declaration scopes, implicit receivers,
     * and additional information applicable either to the context element or its semantic parents.
     *
     * @param smartCasts a set of smart-casts (potentially) available to the context element. Note that the key, [RealVariable], includes
     * stability. Only stable smart casts impact data flow. Check the "Smart cast sink stability" in the Kotlin language specification.
     * Unstable smart casts are still provided for more precise checking and diagnosing.
     */
    class Context(
        val towerDataContext: FirTowerDataContext,
        val smartCasts: Map<RealVariable, Set<ConeKotlinType>>,
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

        val contextProvider = process(file, holder, computeDesignation(file, targetElement), isBodyContextCollected) { candidate ->
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

    fun computeDesignation(file: FirFile, targetElement: PsiElement): FirDesignation? {
        val contextKtDeclaration = targetElement.getNonLocalContainingOrThisDeclaration(::isValidTarget)
        if (contextKtDeclaration != null) {
            val designationPath = FirElementFinder.collectDesignationPath(file, contextKtDeclaration)
            if (designationPath != null) {
                return designationPath
            }
        }

        return null
    }

    private fun isValidTarget(declaration: KtDeclaration): Boolean {
        if (declaration.isAutonomousElement) {
            return true
        }

        return false
    }

    /**
     * Processes the [FirFile], collecting contexts for elements matching the [filter].
     *
     * @param file The file to process.
     * @param holder The [SessionHolder] for the session that owns a [file].
     * @param designation The declaration to process. If `null`, all declarations in the [file] are processed.
     * @param shouldCollectBodyContext If `true`, [ContextKind.BODY] is collected where available.
     * @param filter The filter predicate. Context is collected only for [PsiElement]s for which the [filter] returns `true`.
     */
    fun process(
        file: FirFile,
        holder: SessionHolder,
        designation: FirDesignation?,
        shouldCollectBodyContext: Boolean,
        filter: (PsiElement) -> FilterResponse,
    ): ContextProvider {
        val interceptor = designation?.let(::DesignationInterceptor)
        val visitor = ContextCollectorVisitor(holder, shouldCollectBodyContext, filter, interceptor)
        visitor.collect(file)

        return ContextProvider { element, kind -> visitor[element, kind] }
    }

    fun interface ContextProvider {
        operator fun get(element: PsiElement, kind: ContextKind): Context?
    }
}

private class DesignationInterceptor(val designation: FirDesignation) : () -> FirElement? {
    private val targetIterator = iterator {
        yieldAll(designation.path)
        yield(designation.target)
    }

    override fun invoke(): FirElement? = if (targetIterator.hasNext()) targetIterator.next() else null
}

private class ContextCollectorVisitor(
    private val bodyHolder: SessionHolder,
    private val shouldCollectBodyContext: Boolean,
    private val filter: (PsiElement) -> FilterResponse,
    private val designationPathInterceptor: DesignationInterceptor?,
) : FirDefaultVisitorVoid() {
    fun collect(file: FirFile) {
        if (designationPathInterceptor != null) {
            withInterceptor {
                // This code is unreachable in the case of a not empty path
                errorWithAttachment("Designation path is empty") {
                    withFirEntry("file", file)
                    withFirDesignationEntry("designation", designationPathInterceptor.designation)
                }
            }
        } else {
            file.accept(this)
        }
    }

    private data class ContextKey(val element: PsiElement, val kind: ContextKind)

    operator fun get(element: PsiElement, kind: ContextKind): Context? {
        val key = ContextKey(element, kind)
        return result[key]
    }

    private var isActive = true

    private val parents = ArrayList<FirElement>()

    private val context = BodyResolveContext(
        returnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
        dataFlowAnalyzerContext = DataFlowAnalyzerContext(bodyHolder.session)
    )

    private val result = HashMap<ContextKey, Context>()

    private fun getSessionHolder(declaration: FirDeclaration): SessionHolder {
        return when (val session = declaration.moduleData.session) {
            bodyHolder.session -> bodyHolder
            else -> SessionHolderImpl(session, bodyHolder.scopeSession)
        }
    }

    override fun visitElement(element: FirElement) {
        dumpContext(element, ContextKind.SELF)

        withParent(element) {
            dumpContext(element, ContextKind.BODY)

            onActive {
                element.acceptChildren(this)
            }
        }
    }

    private fun dumpContext(fir: FirElement, kind: ContextKind) {
        ProgressManager.checkCanceled()

        if (kind == ContextKind.BODY && !shouldCollectBodyContext) {
            return
        }

        val psi = fir.psi ?: return

        val key = ContextKey(psi, kind)
        if (key in result) {
            return
        }

        val response = filter(psi)
        if (response != FilterResponse.SKIP) {
            result[key] = computeContext(fir, kind)
        }

        if (response == FilterResponse.STOP) {
            isActive = false
        }
    }

    @OptIn(ImplicitValue.ImplicitValueInternals::class)
    private fun computeContext(fir: FirElement, kind: ContextKind): Context {
        val implicitReceiverStack = context.towerDataContext.implicitValueStorage

        val smartCasts = mutableMapOf<RealVariable, Set<ConeKotlinType>>()

        val cfgNode = getClosestControlFlowNode(fir, kind)

        if (cfgNode != null) {
            val flow = cfgNode.flow

            val realVariables = flow.knownVariables.filterIsInstance<RealVariable>()
                .sortedBy { it.symbol.memberDeclarationNameOrNull?.asString() }

            for (realVariable in realVariables) {
                val typeStatement = flow.getTypeStatement(realVariable) ?: continue
                val stability = realVariable.getStability(flow, bodyHolder.session)
                if (stability != SmartcastStability.STABLE_VALUE && stability != SmartcastStability.CAPTURED_VARIABLE) {
                    continue
                }

                smartCasts[typeStatement.variable as RealVariable] = typeStatement.upperTypes

                // The compiler pushes smart-cast types for implicit receivers to ease later lookups.
                // Here we emulate such behavior. Unlike the compiler, though, modified types are only reflected in the created snapshot.
                // See other usages of 'replaceReceiverType()' for more information.
                if (realVariable.isImplicit) {
                    val smartCastedType = typeStatement.smartCastedType(bodyHolder.session.typeContext)
                    implicitReceiverStack.replaceImplicitValueType(realVariable.symbol, smartCastedType)
                }
            }
        }

        val towerDataContextSnapshot = context.towerDataContext.createSnapshot(keepMutable = true)

        for (realVariable in smartCasts.keys) {
            if (realVariable.isImplicit) {
                implicitReceiverStack.replaceImplicitValueType(realVariable.symbol, realVariable.originalType)
            }
        }

        return Context(towerDataContextSnapshot, smartCasts)
    }

    private fun getClosestControlFlowNode(fir: FirElement, kind: ContextKind): CFGNode<*>? {
        val selfNode = getControlFlowNode(fir, kind)
        if (selfNode != null) {
            return selfNode
        }

        // For some specific elements, such as types or references, there is usually no associated 'CFGNode'.
        for (parent in parents.asReversed()) {
            val parentNode = getControlFlowNode(parent, kind)
            if (parentNode != null) {
                return parentNode
            }
        }

        return null
    }

    private val nodesCache = HashMap<FirControlFlowGraphOwner, Map<FirElement, CFGNode<*>>>()

    /**
     * Returns the first occurrence of an [element] inside the [flow]
     *
     * @param container a [FirControlFlowGraphOwner] where [element] should be searched
     * @param element an [FirElement] to search
     * @param flow an [ControlFlowGraph] from [container]
     */
    private fun findNode(container: FirControlFlowGraphOwner, element: FirElement, flow: ControlFlowGraph): CFGNode<*>? {
        val map = nodesCache.getOrPut(container) { buildDeclarationNodesMapping(flow) }
        return map[element]
    }

    /**
     * @see findNode
     */
    private fun buildDeclarationNodesMapping(
        flow: ControlFlowGraph,
    ): Map<FirElement, CFGNode<*>> = HashMap<FirElement, CFGNode<*>>().apply {
        for (node in flow.nodes) {
            if (isAcceptedControlFlowNode(node)) {
                val fir = node.fir
                // We are interested only in the first one
                putIfAbsent(fir, node)
            }
        }
    }.ifEmpty(::emptyMap)

    private fun getControlFlowNode(fir: FirElement, kind: ContextKind): CFGNode<*>? {
        for (container in context.containers.asReversed()) {
            val cfgOwner = container as? FirControlFlowGraphOwner ?: continue
            val cfgReference = cfgOwner.controlFlowGraphReference ?: continue
            val cfg = cfgReference.controlFlowGraph ?: continue

            val node = findNode(container, fir, cfg)
            when {
                node != null -> return when (kind) {
                    ContextKind.SELF -> {
                        // For the 'SELF' mode, we need to find the state *before* the 'FirElement'
                        node.previousNodes.singleOrNull()?.takeIf { it in cfg.nodes } ?: node
                    }
                    ContextKind.BODY -> {
                        node
                    }
                }
                !cfg.isSubGraph -> return null
            }
        }

        return null
    }

    private fun isAcceptedControlFlowNode(node: CFGNode<*>): Boolean = node !is ClassExitNode

    override fun visitScript(script: FirScript) = withProcessor(script) {
        dumpContext(script, ContextKind.SELF)

        processAnnotations(script)

        onActiveBody {
            val holder = getSessionHolder(script)

            context.withScript(script, holder) {
                dumpContext(script, ContextKind.BODY)

                onActive {
                    withInterceptor {
                        processChildren(script)
                    }
                }
            }
        }
    }

    override fun visitFile(file: FirFile) = withProcessor(file) {
        val holder = getSessionHolder(file)

        context.withFile(file, holder) {
            dumpContext(file, ContextKind.SELF)

            processFileHeader(file)

            onActive {
                withInterceptor {
                    processChildren(file)
                }
            }
        }
    }

    override fun visitCodeFragment(codeFragment: FirCodeFragment) {
        codeFragment.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        val holder = getSessionHolder(codeFragment)

        context.withCodeFragment(codeFragment, holder) {
            super.visitCodeFragment(codeFragment)
        }
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
        dumpContext(annotationCall, ContextKind.SELF)

        onActiveBody {
            dumpContext(annotationCall, ContextKind.BODY)

            // Technically, annotation arguments might contain arbitrary expressions.
            // However, such cases are very rare, as it's currently forbidden in Kotlin.
            // Here we ignore declarations that might be inside such expressions, avoiding unnecessary tree traversal.
        }
    }

    /**
     * If the whole function call looks like `foo().bar()`, then we want
     * the implicit receivers from extensions generated for the `foo()` call
     * to be available at the `bar()` position.
     *
     * That's why we have to accept the children (i.e., the receivers)
     * and call [addReceiversFromExtensions] on them first, and only then to
     * dump the context for the [functionCall] itself.
     *
     * @see addReceiversFromExtensions
     */
    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        onActive {
            withParent(functionCall) {
                functionCall.acceptChildren(this)
            }
        }

        dumpContext(functionCall, ContextKind.SELF)

        context.addReceiversFromExtensions(functionCall, bodyHolder)
    }

    /**
     * If the whole property access call looks like `foo().baz`, then we want
     * the implicit receivers from extensions generated for the `foo()` call
     * to be available at the `baz` position.
     *
     * That's why we have to accept the children (i.e., the receivers)
     * and call [addReceiversFromExtensions] on them first, and only then to
     * dump the context for the [propertyAccessExpression] itself.
     *
     * N.B. [FirPropertyAccessExpression.acceptChildren] implementation visits [FirPropertyAccessExpression.calleeReference]
     * before it visits the receivers.
     * We want to visit `calleeReference` last so its context contains
     * the implicit receivers generated for the receivers' calls.
     *
     * @see addReceiversFromExtensions
     */
    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
        onActive {
            withParent(propertyAccessExpression) {
                val calleeReference = propertyAccessExpression.calleeReference

                propertyAccessExpression.acceptChildren(FilteringVisitor(this, elementsToSkip = setOf(calleeReference)))
                calleeReference.accept(this)
            }
        }

        dumpContext(propertyAccessExpression, ContextKind.SELF)
    }

    override fun visitRegularClass(regularClass: FirRegularClass) = withProcessor(regularClass) {
        dumpContext(regularClass, ContextKind.SELF)

        context.withClassHeader(regularClass) {
            processRawAnnotations(regularClass)
        }

        onActiveBody {
            regularClass.lazyResolveToPhase(FirResolvePhase.STATUS)

            context.withContainingClass(regularClass) {
                processClassHeader(regularClass)

                val holder = getSessionHolder(regularClass)

                context.forRegularClassBody(regularClass, holder) {
                    dumpContext(regularClass, ContextKind.BODY)

                    onActive {
                        withInterceptor {
                            processChildren(regularClass)
                        }
                    }
                }
            }
        }

        if (regularClass.isLocal) {
            context.storeClassIfNotNested(regularClass, regularClass.moduleData.session)
        }
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias) {
        context.forTypeAlias(typeAlias) {
            super.visitTypeAlias(typeAlias)
        }
    }

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop) = withProcessor(doWhileLoop) {
        dumpContext(doWhileLoop, ContextKind.SELF)

        onActiveBody {
            dumpContext(doWhileLoop, ContextKind.BODY)

            context.forBlock(bodyHolder.session) {
                process(doWhileLoop.block) { block ->
                    doVisitBlock(block, isolateBlock = false)
                }

                process(doWhileLoop.condition)
            }

            processChildren(doWhileLoop)
        }
    }

    /**
     * Process the parts of the class declaration which resolution is not affected
     * by the class own supertypes.
     *
     * Processing those parts before adding the implicit receiver of the class
     * to the [context] allows to not collect incorrect contexts for them later on.
     */
    @OptIn(PrivateForInline::class)
    private fun Processor.processClassHeader(regularClass: FirRegularClass) {
        context.withClassHeader(regularClass) {
            context.withTypeParametersOf(regularClass) {
                processList(regularClass.contextParameters)
                processList(regularClass.typeParameters)
                processList(regularClass.superTypeRefs)
            }
        }
    }

    private fun Processor.processFileHeader(file: FirFile) {
        process(file.packageDirective)
        processList(file.imports)
        processRawAnnotations(file)
    }

    /**
     * Same as [processClassHeader], but for anonymous objects.
     *
     * N.B. Anonymous classes cannot have its own explicit type parameters, so we do not process them.
     */
    private fun Processor.processAnonymousObjectHeader(anonymousObject: FirAnonymousObject) {
        processList(anonymousObject.superTypeRefs)
    }

    override fun visitErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor) {
        visitConstructor(errorPrimaryConstructor)
    }

    override fun visitConstructor(constructor: FirConstructor) = withProcessor(constructor) {
        dumpContext(constructor, ContextKind.SELF)

        context.forConstructor(constructor) {
            processRawAnnotations(constructor)

            onActiveBody {
                constructor.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)


                val holder = getSessionHolder(constructor)
                val containingClass = context.containerIfAny as? FirRegularClass

                context.forConstructorParameters(constructor, containingClass, holder) {
                    processList(constructor.valueParameters)
                }

                context.forConstructorBody(constructor, holder.session) {
                    processList(constructor.valueParameters)

                    dumpContext(constructor, ContextKind.BODY)

                    onActive {
                        process(constructor.body)
                    }
                }

                onActive {
                    context.forDelegatedConstructorCallChildren(constructor, owningClass = null, holder) {
                        process(constructor.delegatedConstructor)
                    }

                    onActive {
                        processChildren(constructor)
                    }
                }
            }
        }
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry) = withProcessor(enumEntry) {
        dumpContext(enumEntry, ContextKind.SELF)

        onActiveBody {
            // We have to wrap annotation processing into withEnumEntry as well as it provides the correct context
            // Otherwise there will be the enum entry as an implicit receiver
            context.withEnumEntry(enumEntry) {
                processRawAnnotations(enumEntry)

                onActiveBody {
                    enumEntry.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                    dumpContext(enumEntry, ContextKind.BODY)

                    onActive {
                        processChildren(enumEntry)
                    }
                }
            }
        }
    }

    override fun visitDanglingModifierList(danglingModifierList: FirDanglingModifierList) = withProcessor(danglingModifierList) {
        dumpContext(danglingModifierList, ContextKind.SELF)

        processAnnotations(danglingModifierList)

        onActiveBody {
            danglingModifierList.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            context.withDanglingModifierList(danglingModifierList) {
                dumpContext(danglingModifierList, ContextKind.BODY)
                onActive {
                    processChildren(danglingModifierList)
                }
            }
        }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) = withProcessor(simpleFunction) {
        dumpContext(simpleFunction, ContextKind.SELF)

        processAnnotations(simpleFunction)

        onActiveBody {
            simpleFunction.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            val holder = getSessionHolder(simpleFunction)

            context.withSimpleFunction(simpleFunction, holder.session) {
                processList(simpleFunction.typeParameters)
                onActive {
                    process(simpleFunction.receiverParameter)
                }

                onActiveBody {
                    context.forFunctionBody(simpleFunction, holder) {
                        dumpContext(simpleFunction, ContextKind.BODY)

                        processList(simpleFunction.contextParameters)
                        processList(simpleFunction.valueParameters)

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
    }

    override fun visitErrorProperty(errorProperty: FirErrorProperty) {
        visitProperty(errorProperty)
    }

    override fun visitProperty(property: FirProperty) = withProcessor(property) {
        dumpContext(property, ContextKind.SELF)

        processAnnotations(property)

        onActiveBody {
            property.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            context.withProperty(property) {
                processList(property.typeParameters)
                onActive {
                    process(property.receiverParameter)
                }

                onActiveBody {
                    dumpContext(property, ContextKind.BODY)
                }

                onActive {
                    context.withParameters(property, getSessionHolder(property)) {
                        processList(property.contextParameters)
                    }

                    onActive {
                        context.forPropertyInitializerIfNonLocal(property) {
                            process(property.initializer)

                            onActive {
                                process(property.delegate)

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
            }
        }

        if (property.isLocal) {
            context.storeVariable(property, property.moduleData.session)
        }
    }

    /**
     * Executes [f] wrapped with [BodyResolveContext.forPropertyInitializer] if the [property] is not local.
     * Note that [BodyResolveContext.forPropertyInitializer] performs the tower data cleanup in the [BodyResolveContext].
     *
     * Otherwise, just calls [f] with no the cleanup.
     *
     * We need to disable the context cleanup for local properties
     * to preserve the implicit receivers introduced by the [addReceiversFromExtensions].
     */
    private fun BodyResolveContext.forPropertyInitializerIfNonLocal(property: FirProperty, f: () -> Unit) {
        if (!property.isLocal) {
            forPropertyInitializer(f)
        } else {
            f()
        }
    }

    private fun FirExpression.unwrap(): FirExpression? {
        return when (this) {
            is FirCheckNotNullCall -> argument.unwrap()
            is FirSafeCallExpression -> (selector as? FirExpression)?.unwrap()
            else -> this
        }
    }

    /**
     * We visit fields to properly handle supertypes delegation:
     *
     * ```kt
     * class Foo : Bar by baz
     * ```
     *
     * In the code above, `baz` expression is saved into a separate synthetic field.
     * It's not accessible from the delegated constructor, it's just added to the
     * `Foo` class body.
     */
    override fun visitField(field: FirField) = withProcessor(field) {
        dumpContext(field, ContextKind.SELF)

        processAnnotations(field)

        onActiveBody {
            field.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            context.withField(field) {
                dumpContext(field, ContextKind.BODY)

                onActive {
                    process(field.initializer)
                }
            }
        }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) = withProcessor(propertyAccessor) {
        dumpContext(propertyAccessor, ContextKind.SELF)

        processAnnotations(propertyAccessor)

        onActiveBody {
            val holder = getSessionHolder(propertyAccessor)

            context.withPropertyAccessor(propertyAccessor.propertySymbol.fir, propertyAccessor, holder) {
                dumpContext(propertyAccessor, ContextKind.BODY)

                onActive {
                    processChildren(propertyAccessor)
                }
            }
        }
    }

    override fun visitValueParameter(valueParameter: FirValueParameter) = withProcessor(valueParameter) {
        dumpContext(valueParameter, ContextKind.SELF)

        processAnnotations(valueParameter)

        onActiveBody {
            context.withValueParameter(valueParameter, valueParameter.moduleData.session) {
                dumpContext(valueParameter, ContextKind.BODY)

                onActive {
                    processChildren(valueParameter)
                }
            }
        }
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) = withProcessor(anonymousInitializer) {
        dumpContext(anonymousInitializer, ContextKind.SELF)

        processAnnotations(anonymousInitializer)

        onActiveBody {
            context.withAnonymousInitializer(anonymousInitializer, anonymousInitializer.moduleData.session) {
                dumpContext(anonymousInitializer, ContextKind.BODY)

                onActive {
                    anonymousInitializer.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                    processChildren(anonymousInitializer)
                }
            }
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) = withProcessor(anonymousFunction) {
        dumpContext(anonymousFunction, ContextKind.SELF)

        processAnnotations(anonymousFunction)

        onActiveBody {
            @OptIn(PrivateForInline::class)
            context.withTypeParametersOf(anonymousFunction) {
                processList(anonymousFunction.typeParameters)
                onActive {
                    process(anonymousFunction.receiverParameter)
                }

                onActiveBody {
                    context.withAnonymousFunction(anonymousFunction, bodyHolder) {
                        for (contextParameter in anonymousFunction.contextParameters) {
                            context.storeValueParameterIfNeeded(contextParameter, bodyHolder.session)
                        }

                        for (valueParameter in anonymousFunction.valueParameters) {
                            context.storeValueParameterIfNeeded(valueParameter, bodyHolder.session)
                        }

                        dumpContext(anonymousFunction, ContextKind.BODY)

                        processList(anonymousFunction.contextParameters)
                        processList(anonymousFunction.valueParameters)

                        onActive {
                            process(anonymousFunction.body)
                        }
                    }

                    onActive {
                        processChildren(anonymousFunction)
                    }
                }
            }
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) = withProcessor(anonymousObject) {
        dumpContext(anonymousObject, ContextKind.SELF)

        processAnnotations(anonymousObject)

        onActiveBody {
            processAnonymousObjectHeader(anonymousObject)

            context.withAnonymousObject(anonymousObject, bodyHolder) {
                dumpContext(anonymousObject, ContextKind.BODY)

                onActive {
                    processChildren(anonymousObject)
                }
            }
        }
    }

    override fun visitBlock(block: FirBlock) {
        doVisitBlock(block)
    }

    /**
     * @param isolateBlock whether to wrap the block into a [BodyResolveContext.forBlock]
     */
    private fun doVisitBlock(block: FirBlock, isolateBlock: Boolean = true) = withProcessor(block) {
        dumpContext(block, ContextKind.SELF)

        onActiveBody {
            if (isolateBlock) {
                context.forBlock(bodyHolder.session) {
                    processBlockBody(block)
                }
            } else {
                processBlockBody(block)
            }
        }
    }

    private fun Processor.processBlockBody(block: FirBlock) {
        processChildren(block)
        dumpContext(block, ContextKind.BODY)
    }

    /**
     * Iterates directly through annotations without any [context] adjustments.
     *
     * This function is needed in the case if special modification of [context] is required.
     * In this case such adjustments have to be done before this function.
     *
     * [processAnnotations] should be used by default.
     *
     * @see processAnnotations
     */
    @ContextCollectorDsl
    private fun Processor.processRawAnnotations(declaration: FirDeclaration) {
        for (annotation in declaration.annotations) {
            onActive {
                process(annotation)
            }
        }
    }

    /**
     * Processes [FirDeclaration.annotations] in the context of [declaration].
     *
     * @see org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsTransformer
     */
    @ContextCollectorDsl
    private fun Processor.processAnnotations(declaration: FirDeclaration) {
        @OptIn(PrivateForInline::class)
        context.withContainer(declaration) {
            processRawAnnotations(declaration)
        }
    }

    private inline fun withProcessor(parent: FirElement, block: Processor.() -> Unit) {
        withParent(parent) {
            Processor(this).block()
        }
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
        fun <T : FirElement?> process(element: T?, customVisit: (T & Any) -> Unit) {
            if (element != null) {
                customVisit(element)
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

    /**
     * Ensures that the visitor is going through the path specified by the initial [FirDesignation].
     *
     * If the designation is over, then allows the [block] code to take control.
     */
    private fun withInterceptor(block: () -> Unit) {
        val target = designationPathInterceptor?.invoke()
        if (target != null) {
            target.accept(this)
        } else {
            block()
        }
    }

    private inline fun withParent(parent: FirElement, block: () -> Unit) {
        parents.add(parent)
        try {
            block()
        } finally {
            parents.removeLast()
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
