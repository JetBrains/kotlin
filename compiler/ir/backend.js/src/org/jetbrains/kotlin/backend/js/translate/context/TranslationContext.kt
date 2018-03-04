/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.js.translate.context

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.backend.js.translate.declaration.ClassModelGenerator
import org.jetbrains.kotlin.backend.js.translate.intrinsic.Intrinsics
import org.jetbrains.kotlin.backend.js.translate.reference.CallExpressionTranslator
import org.jetbrains.kotlin.backend.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.backend.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.backend.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.backend.js.translate.utils.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.serialization.js.ModuleKind

import java.util.*

import org.jetbrains.kotlin.js.descriptorUtils.isCoroutineLambda
import org.jetbrains.kotlin.js.descriptorUtils.shouldBeExported
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isNativeObject
import org.jetbrains.kotlin.backend.js.translate.utils.BindingUtils.getDescriptorForElement
import org.jetbrains.kotlin.backend.js.translate.utils.JsAstUtils.pureFqn

/**
 * All the info about the state of the translation process.
 */
class TranslationContext private constructor(
    val parent: TranslationContext?,
    private val staticContext: StaticContext,
    private val dynamicContext: DynamicContext,
    private val aliasingContext: AliasingContext,
    private val usageTracker: UsageTracker?,
    val declarationDescriptor: DeclarationDescriptor?
) {
    val classDescriptor: ClassDescriptor?
    val continuationParameterDescriptor: VariableDescriptor?

    var inlineFunctionContext: InlineFunctionContext? = null
        private set

    private val expressionToTempConstVariableCache = HashMap<JsExpression, TemporaryConstVariable>()

    val config: JsConfig
        get() = staticContext.config

    val currentBlock: JsBlock
        get() = dynamicContext.jsBlock()

    private val outerLocalClassDepth: Int
        get() {
            if (usageTracker == null) return 0
            val capturingDescriptor = usageTracker.containingDescriptor as? ClassDescriptor ?: return 0

            var currentDescriptor: ClassDescriptor? = classDescriptor ?: return 0

            var depth = 0
            while (currentDescriptor !== capturingDescriptor) {
                val container = currentDescriptor!!.containingDeclaration as? ClassDescriptor ?: return 0
                currentDescriptor = container
                depth++
            }
            return depth
        }

    val isPublicInlineFunction: Boolean
        get() = if (inlineFunctionContext == null) false else (inlineFunctionContext!!.descriptor).shouldBeExported(config)

    val currentModule: ModuleDescriptor
        get() = staticContext.currentModule

    val sourceFilePathResolver: SourceFilePathResolver
        get() = staticContext.sourceFilePathResolver

    init {
        if (declarationDescriptor is ClassDescriptor) {
            this.classDescriptor = declarationDescriptor
        } else {
            this.classDescriptor = parent?.classDescriptor
        }

        continuationParameterDescriptor = calculateContinuationParameter()
        inlineFunctionContext = parent?.inlineFunctionContext

        val parentDescriptor = parent?.declarationDescriptor
        if (parentDescriptor !== declarationDescriptor &&
            declarationDescriptor is CallableDescriptor &&
            InlineUtil.isInline(declarationDescriptor)
        ) {
            inlineFunctionContext = InlineFunctionContext((declarationDescriptor as CallableDescriptor?)!!)
        }
    }

    private fun calculateContinuationParameter(): VariableDescriptor? {
        if (parent != null && parent.declarationDescriptor === declarationDescriptor) {
            return parent.continuationParameterDescriptor
        }
        if (declarationDescriptor is FunctionDescriptor) {
            val function = declarationDescriptor as FunctionDescriptor?
            if (function!!.isSuspend) {
                val continuationDescriptor = currentModule.findContinuationClassDescriptor(NoLookupLocation.FROM_BACKEND)

                return LocalVariableDescriptor(
                    declarationDescriptor,
                    Annotations.EMPTY,
                    Name.identifier("continuation"),
                    continuationDescriptor.defaultType,
                    SourceElement.NO_SOURCE
                )
            }
        }
        return null
    }

    fun usageTracker(): UsageTracker? {
        return usageTracker
    }

    fun dynamicContext(): DynamicContext {
        return dynamicContext
    }

    fun contextWithScope(`fun`: JsFunction): TranslationContext {
        return this.newFunctionBody(`fun`, aliasingContext, declarationDescriptor)
    }

    private fun newFunctionBody(
        `fun`: JsFunction, aliasingContext: AliasingContext?,
        descriptor: DeclarationDescriptor?
    ): TranslationContext {
        var aliasingContext = aliasingContext
        val dynamicContext = DynamicContext.newContext(`fun`.scope, `fun`.body)
        if (aliasingContext == null) {
            aliasingContext = this.aliasingContext.inner()
        }

        return TranslationContext(this, this.staticContext, dynamicContext, aliasingContext!!, this.usageTracker, descriptor)
    }

    fun newFunctionBodyWithUsageTracker(`fun`: JsFunction, descriptor: MemberDescriptor): TranslationContext {
        val dynamicContext = DynamicContext.newContext(`fun`.scope, `fun`.body)
        val usageTracker = UsageTracker(this.usageTracker, descriptor)
        return TranslationContext(this, this.staticContext, dynamicContext, this.aliasingContext.inner(), usageTracker, descriptor)
    }

    fun innerWithUsageTracker(descriptor: MemberDescriptor): TranslationContext {
        val usageTracker = UsageTracker(this.usageTracker, descriptor)
        return TranslationContext(this, staticContext, dynamicContext, aliasingContext.inner(), usageTracker, descriptor)
    }

    fun inner(descriptor: MemberDescriptor): TranslationContext {
        return TranslationContext(this, staticContext, dynamicContext, aliasingContext.inner(), usageTracker, descriptor)
    }

    @JvmOverloads
    fun innerBlock(block: JsBlock = JsBlock()): TranslationContext {
        return TranslationContext(
            this, staticContext, dynamicContext.innerBlock(block), aliasingContext, usageTracker,
            this.declarationDescriptor
        )
    }

    fun newDeclaration(descriptor: DeclarationDescriptor): TranslationContext {
        var innerBlock = getBlockForDescriptor(descriptor)
        if (innerBlock == null) {
            innerBlock = dynamicContext.jsBlock()
        }
        val dynamicContext = DynamicContext.newContext(getScopeForDescriptor(descriptor), innerBlock!!)
        return TranslationContext(this, staticContext, dynamicContext, aliasingContext, usageTracker, descriptor)
    }

    private fun innerWithAliasingContext(aliasingContext: AliasingContext): TranslationContext {
        return TranslationContext(this, staticContext, dynamicContext, aliasingContext, usageTracker, declarationDescriptor)
    }

    fun innerContextWithAliased(correspondingDescriptor: DeclarationDescriptor, alias: JsExpression): TranslationContext {
        return this.innerWithAliasingContext(aliasingContext.inner(correspondingDescriptor, alias))
    }

    fun innerContextWithAliasesForExpressions(aliases: Map<KtExpression, JsExpression>): TranslationContext {
        return if (aliases.isEmpty()) this else this.innerWithAliasingContext(aliasingContext.withExpressionsAliased(aliases))
    }

    fun innerContextWithDescriptorsAliased(aliases: Map<DeclarationDescriptor, JsExpression>): TranslationContext {
        return if (aliases.isEmpty()) this else this.innerWithAliasingContext(aliasingContext.withDescriptorsAliased(aliases))
    }

    private fun getBlockForDescriptor(descriptor: DeclarationDescriptor): JsBlock? {
        return if (descriptor is CallableDescriptor) {
            getFunctionObject(descriptor as CallableDescriptor).body
        } else {
            null
        }
    }

    fun bindingContext(): BindingContext {
        return staticContext.bindingContext
    }

    fun bindingTrace(): BindingTrace {
        return staticContext.bindingTrace
    }

    fun getScopeForDescriptor(descriptor: DeclarationDescriptor): JsScope {
        return staticContext.getScopeForDescriptor(descriptor)
    }

    fun getNameForElement(element: PsiElement): JsName {
        val descriptor = getDescriptorForElement(bindingContext(), element)
        return getNameForDescriptor(descriptor)
    }

    fun getNameForDescriptor(descriptor: DeclarationDescriptor): JsName {
        return staticContext.getNameForDescriptor(descriptor)
    }

    fun getInnerNameForDescriptor(descriptor: DeclarationDescriptor): JsName {
        return staticContext.getInnerNameForDescriptor(descriptor)
    }

    fun getInlineableInnerNameForDescriptor(descriptor: DeclarationDescriptor): JsName {
        var name: JsName?
        if (inlineFunctionContext == null || !isPublicInlineFunction || !descriptor.shouldBeExported(config) ||
            DescriptorUtils.isAncestor(inlineFunctionContext!!.descriptor, descriptor, false)
        ) {
            name = getInnerNameForDescriptor(descriptor)
        } else {
            val tag = staticContext.getTag(descriptor)
            name = inlineFunctionContext!!.imports[tag]
            if (name == null) {
                name = createInlineableInnerNameForDescriptor(descriptor)!!
                inlineFunctionContext!!.imports[tag!!] = name
            }
        }

        return name
    }

    private fun createInlineableInnerNameForDescriptor(descriptor: DeclarationDescriptor): JsName? {
        assert(inlineFunctionContext != null)

        val imported = createInlineLocalImportExpression(descriptor)
        if (imported is JsNameRef) {
            if (imported.qualifier == null && imported.ident == Namer.rootPackageName &&
                (descriptor is PackageFragmentDescriptor || descriptor is ModuleDescriptor)
            ) {
                return imported.name
            }
        }

        val result = JsScope.declareTemporaryName(StaticContext.getSuggestedName(descriptor))
        if (isFromCurrentModule(descriptor) && !AnnotationsUtils.isNativeObject(descriptor)) {
            result.localAlias = getInnerNameForDescriptor(descriptor)
        }
        result.descriptor = descriptor
        result.staticRef = imported
        result.imported = true
        inlineFunctionContext!!.importBlock.statements.add(JsAstUtils.newVar(result, imported))

        return result
    }

    fun getReferenceToIntrinsic(intrinsicName: String): JsExpression {
        val result: JsExpression
        if (inlineFunctionContext == null || !isPublicInlineFunction) {
            result = staticContext.getReferenceToIntrinsic(intrinsicName)
        } else {
            val tag = "intrinsic:" + intrinsicName
            result = pureFqn((inlineFunctionContext!!.imports as java.util.Map<String, JsName>).computeIfAbsent(tag) { t ->
                val imported = TranslationUtils.getIntrinsicFqn(intrinsicName)

                val name = JsScope.declareTemporaryName(NameSuggestion.sanitizeName(intrinsicName))
                name.imported = true
                inlineFunctionContext!!.importBlock.statements.add(JsAstUtils.newVar(name, imported))
                name
            }, null)
        }

        return result
    }

    fun getNameForObjectInstance(descriptor: ClassDescriptor): JsName {
        return staticContext.getNameForObjectInstance(descriptor)
    }

    fun getQualifiedReference(descriptor: DeclarationDescriptor): JsExpression {
        var result: JsExpression = staticContext.getQualifiedReference(descriptor)
        if (isPublicInlineFunction) {
            if (isFromCurrentModule(descriptor)) {
                if (descriptor is MemberDescriptor) {
                    staticContext.export(descriptor, true)
                }
            } else {
                val module = DescriptorUtils.getContainingModule(descriptor)
                if (module !== staticContext.currentModule && !isInlineFunction(descriptor)) {
                    val replacement = staticContext.exportModuleForInline(module)
                    if (replacement != null) {
                        result = replaceModuleReference(result, getInnerNameForDescriptor(module), replacement)
                    }
                }
            }
        }
        return result
    }

    private fun isInlineFunction(descriptor: DeclarationDescriptor): Boolean {
        return if (descriptor !is CallableDescriptor) false else CallExpressionTranslator.shouldBeInlined(
            descriptor as CallableDescriptor,
            this
        )
    }

    fun getInnerReference(descriptor: DeclarationDescriptor): JsExpression {
        return pureFqn(getInlineableInnerNameForDescriptor(descriptor), null)
    }

    private fun createInlineLocalImportExpression(descriptor: DeclarationDescriptor): JsExpression {
        var result = getQualifiedReference(descriptor)
        val name = getInnerNameForDescriptor(descriptor)

        val suggested = staticContext.suggestName(descriptor)
        if (suggested != null && config.moduleKind != ModuleKind.PLAIN && isPublicInlineFunction) {
            val moduleId = AnnotationsUtils.getModuleName(suggested.descriptor)
            if (moduleId != null) {
                val replacement = staticContext.exportModuleForInline(moduleId, name)
                result = replaceModuleReference(result, name, replacement)
            } else if (isNativeObject(suggested.descriptor) && DescriptorUtils.isTopLevelDeclaration(suggested.descriptor)) {
                val fileModuleId = AnnotationsUtils.getFileModuleName(bindingContext(), suggested.descriptor)
                if (fileModuleId != null) {
                    val fileModuleName = staticContext.getImportedModule(fileModuleId, null).internalName
                    val replacement = staticContext.exportModuleForInline(fileModuleId, fileModuleName)
                    result = replaceModuleReference(staticContext.getQualifiedReference(descriptor), fileModuleName, replacement)
                }
            }
        }

        return result
    }

    fun getNameForBackingField(property: VariableDescriptorWithAccessors): JsName {
        return staticContext.getNameForBackingField(property)
    }

    fun declareTemporary(initExpression: JsExpression?, source: Any?): TemporaryVariable {
        return dynamicContext.declareTemporary(initExpression, source)
    }

    fun defineTemporary(initExpression: JsExpression): JsExpression {
        val `var` = dynamicContext.declareTemporary(initExpression, initExpression.source)
        addStatementToCurrentBlock(`var`.assignmentStatement())
        return `var`.reference()
    }

    fun cacheExpressionIfNeeded(expression: JsExpression): JsExpression {
        return if (TranslationUtils.isCacheNeeded(expression)) defineTemporary(expression) else expression
    }

    fun getOrDeclareTemporaryConstVariable(expression: JsExpression): TemporaryConstVariable {
        var tempVar: TemporaryConstVariable? = expressionToTempConstVariableCache[expression]

        if (tempVar == null) {
            val tmpVar = declareTemporary(expression, expression.source)

            tempVar = TemporaryConstVariable(tmpVar.name(), tmpVar.assignmentExpression())

            expressionToTempConstVariableCache[expression] = tempVar
            expressionToTempConstVariableCache[tmpVar.assignmentExpression()] = tempVar
        }

        return tempVar
    }

    fun namer(): Namer {
        return staticContext.namer
    }

    fun intrinsics(): Intrinsics {
        return staticContext.intrinsics
    }

    fun program(): JsProgram {
        return staticContext.program
    }

    fun scope(): JsScope {
        return dynamicContext.scope
    }

    fun aliasingContext(): AliasingContext {
        return aliasingContext
    }

    fun getFunctionObject(descriptor: CallableDescriptor): JsFunction {
        return staticContext.getFunctionWithScope(descriptor)
    }

    fun addStatementToCurrentBlock(statement: JsStatement) {
        dynamicContext.jsBlock().statements.add(statement)
    }

    fun addStatementsToCurrentBlock(statements: Collection<JsStatement>) {
        dynamicContext.jsBlock().statements.addAll(statements)
    }

    fun addStatementsToCurrentBlockFrom(context: TranslationContext) {
        addStatementsToCurrentBlockFrom(context.dynamicContext().jsBlock())
    }

    fun addStatementsToCurrentBlockFrom(block: JsBlock) {
        dynamicContext.jsBlock().statements.addAll(block.statements)
    }

    fun currentBlockIsEmpty(): Boolean {
        return dynamicContext.jsBlock().isEmpty
    }

    fun moveVarsFrom(context: TranslationContext) {
        dynamicContext.moveVarsFrom(context.dynamicContext())
    }

    fun getAliasForDescriptor(descriptor: DeclarationDescriptor): JsExpression? {
        val nameRef = captureIfNeedAndGetCapturedName(descriptor)
        if (nameRef != null) {
            return nameRef
        }

        val alias = aliasingContext.getAliasForDescriptor(descriptor)
        return alias?.deepCopy()
    }

    fun getDispatchReceiver(descriptor: ReceiverParameterDescriptor): JsExpression {
        val alias = getAliasForDescriptor(descriptor)
        if (alias != null) {
            return alias
        }
        if (descriptor.containingDeclaration.isCoroutineLambda) {
            val result = JsNameRef("$\$controller$$", JsAstUtils.stateMachineReceiver())
            result.coroutineController = true
            return result
        }

        if (DescriptorUtils.isObject(descriptor.containingDeclaration)) {
            if (isConstructorOrDirectScope(descriptor.containingDeclaration)) {
                return JsThisRef()
            } else {
                val objectDescriptor = descriptor.containingDeclaration as ClassDescriptor
                return ReferenceTranslator.translateAsValueReference(objectDescriptor, this)
            }
        }

        if (descriptor.value is ExtensionReceiver) return JsThisRef()

        val classifier = descriptor.value.type.constructor.declarationDescriptor

        // TODO: can't tell why this assertion is valid, revisit this code later
        assert(classifier is ClassDescriptor)

        val cls = classifier as ClassDescriptor?
        assert(classDescriptor != null)
        return getDispatchReceiverPath(classDescriptor!!, cls!!, JsThisRef())
    }

    private fun isConstructorOrDirectScope(descriptor: DeclarationDescriptor): Boolean {
        return descriptor === DescriptorUtils.getParentOfType(declarationDescriptor, ClassDescriptor::class.java, false)
    }

    private fun getDispatchReceiverPath(
        from: ClassDescriptor, to: ClassDescriptor,
        qualifier: JsExpression
    ): JsExpression {
        var from = from
        var qualifier = qualifier
        while (true) {
            val alias = getAliasForDescriptor(from.thisAsReceiverParameter)
            if (alias != null) {
                qualifier = alias
            }

            if (from === to || !from.isInner || from.containingDeclaration !is ClassDescriptor) {
                break
            }

            qualifier = JsNameRef(Namer.OUTER_FIELD_NAME, qualifier)
            from = from.containingDeclaration as ClassDescriptor
        }

        return qualifier
    }

    private fun captureIfNeedAndGetCapturedName(descriptor: DeclarationDescriptor): JsExpression? {
        if (usageTracker != null) {
            usageTracker.used(descriptor)

            val name = usageTracker.getNameForCapturedDescriptor(descriptor)
            if (name != null) return getCapturedReference(name)
        }

        return null
    }

    fun captureTypeIfNeedAndGetCapturedName(descriptor: TypeParameterDescriptor): JsExpression? {
        if (usageTracker == null) return null

        usageTracker.used(descriptor)

        val name = usageTracker!!.capturedTypes[descriptor]
        return if (name != null) getCapturedReference(name) else null
    }

    fun getCapturedTypeName(descriptor: TypeParameterDescriptor): JsName {
        var result: JsName? = if (usageTracker != null) usageTracker!!.capturedTypes[descriptor] else null
        if (result == null) {
            result = getNameForDescriptor(descriptor)
        }

        return result
    }

    private fun getCapturedReference(name: JsName): JsExpression {
        var result: JsExpression
        if (shouldCaptureViaThis()) {
            result = JsThisRef()
            val depth = outerLocalClassDepth
            for (i in 0 until depth) {
                result = JsNameRef(Namer.OUTER_FIELD_NAME, result)
            }
            result = JsNameRef(name, result)
        } else {
            result = name.makeRef()
        }
        return result
    }

    private fun shouldCaptureViaThis(): Boolean {
        if (declarationDescriptor == null) return false

        if (DescriptorUtils.isDescriptorWithLocalVisibility(declarationDescriptor)) return false
        return if (declarationDescriptor is ConstructorDescriptor && DescriptorUtils.isDescriptorWithLocalVisibility(declarationDescriptor!!.containingDeclaration)) false else true

    }

    fun putClassOrConstructorClosure(descriptor: MemberDescriptor, closure: List<DeclarationDescriptor>) {
        staticContext.putClassOrConstructorClosure(descriptor, closure)
    }

    fun getClassOrConstructorClosure(classOrConstructor: MemberDescriptor): List<DeclarationDescriptor>? {
        if (classOrConstructor is TypeAliasConstructorDescriptor) {
            val constructorDescriptor = classOrConstructor.underlyingConstructorDescriptor
            return getClassOrConstructorClosure(constructorDescriptor)
        }

        var result = staticContext.getClassOrConstructorClosure(classOrConstructor)
        if (result == null &&
            classOrConstructor is ConstructorDescriptor &&
            (classOrConstructor as ConstructorDescriptor).isPrimary
        ) {
            result = staticContext.getClassOrConstructorClosure(classOrConstructor.containingDeclaration as ClassDescriptor)
        }
        return result
    }

    /**
     * Gets an expression to pass to a constructor of a closure function. I.e. consider the case:
     *
     * ```
     * fun a(x) {
     * fun b(y) = x + y
     * return b
     * }
     * ```
     *
     * Here, `x` is a free variable of `b`. Transform `a` into the following form:
     *
     * ```
     * fun a(x) {
     * fun b0(x0) = { y -> x0 * y }
     * return b0(x)
     * }
     * ```
     *
     * This function generates arguments passed to newly generated `b0` closure, as well as for the similar case of local class and
     * object expression.
     *
     * @param descriptor represents a free variable or, more generally, free declaration.
     * @return expression to pass to a closure constructor.
     */
    fun getArgumentForClosureConstructor(descriptor: DeclarationDescriptor): JsExpression {
        val alias = getAliasForDescriptor(descriptor)
        if (alias != null) return alias
        if (descriptor is ReceiverParameterDescriptor) {
            return getDispatchReceiver(descriptor)
        }
        return if (descriptor.isCoroutineLambda) {
            JsThisRef()
        } else getNameForDescriptor(descriptor).makeRef()
    }

    fun getTypeArgumentForClosureConstructor(descriptor: TypeParameterDescriptor): JsExpression {
        var captured: JsExpression? = null
        if (usageTracker != null) {
            val name = usageTracker!!.capturedTypes[descriptor]
            if (name != null) {
                captured = name.makeRef()
            }
        }

        return if (captured != null) captured else getNameForDescriptor(descriptor).makeRef()
    }

    fun getOuterClassReference(descriptor: ClassDescriptor): JsName? {
        val container = descriptor.containingDeclaration
        return if (container !is ClassDescriptor || !descriptor.isInner) {
            null
        } else staticContext.getScopeForDescriptor(descriptor).declareName(Namer.OUTER_FIELD_NAME)

    }

    fun startDeclaration() {
        val classDescriptor = this.classDescriptor
        if (classDescriptor != null && classDescriptor!!.containingDeclaration !is ClassOrPackageFragmentDescriptor) {
            staticContext.deferredCallSites.put(classDescriptor, ArrayList<DeferredCallSite>())
        }
    }

    fun endDeclaration(): List<DeferredCallSite> {
        var result: List<DeferredCallSite>? = null
        if (classDescriptor != null) {
            result = staticContext.deferredCallSites.remove(classDescriptor)
        }
        if (result == null) {
            result = emptyList()
        }
        return result
    }

    fun shouldBeDeferred(constructor: ClassConstructorDescriptor): Boolean {
        val classDescriptor = constructor.containingDeclaration
        return staticContext.deferredCallSites.containsKey(classDescriptor)
    }

    fun deferConstructorCall(constructor: ClassConstructorDescriptor, invocationArgs: MutableList<JsExpression>) {
        val classDescriptor = constructor.containingDeclaration
        val callSites = staticContext.deferredCallSites[classDescriptor] ?: throw IllegalStateException(
            "This method should be call only when `shouldBeDeferred` method " +
                    "reports true for given constructor: " + constructor
        )
        callSites.add(DeferredCallSite(constructor, invocationArgs, this))
    }

    fun addInlineCall(descriptor: CallableDescriptor) {
        staticContext.addInlineCall(descriptor)
    }

    fun addDeclarationStatement(statement: JsStatement) {
        if (inlineFunctionContext != null) {
            inlineFunctionContext!!.declarationsBlock.statements.add(statement)
        } else {
            staticContext.declarationStatements.add(statement)
        }
    }

    fun addTopLevelStatement(statement: JsStatement) {
        staticContext.topLevelStatements.add(statement)
    }

    fun createRootScopedFunction(descriptor: DeclarationDescriptor): JsFunction {
        return createRootScopedFunction(descriptor.toString())
    }

    fun createRootScopedFunction(description: String): JsFunction {
        return JsFunction(staticContext.fragment.scope, JsBlock(), description)
    }

    fun addClass(classDescriptor: ClassDescriptor) {
        if (inlineFunctionContext != null) {
            val classModel = ClassModelGenerator(this).generateClassModel(classDescriptor)
            val targetStatements = inlineFunctionContext!!.prototypeBlock.statements
            val superName = classModel.superName
            if (superName != null) {
                targetStatements.addAll(createPrototypeStatements(superName, classModel.name))
            }
            targetStatements.addAll(classModel.postDeclarationBlock.statements)
        } else {
            staticContext.addClass(classDescriptor)
        }
    }

    fun export(descriptor: MemberDescriptor) {
        staticContext.export(descriptor, false)
    }

    fun isFromCurrentModule(descriptor: DeclarationDescriptor): Boolean {
        return staticContext.currentModule === descriptor.module
    }

    fun getNameForSpecialFunction(function: SpecialFunction): JsName {
        if (inlineFunctionContext == null || !isPublicInlineFunction) {
            return staticContext.getNameForSpecialFunction(function)
        } else {
            val tag = TranslationUtils.getTagForSpecialFunction(function)
            return inlineFunctionContext!!.imports.getOrPut(tag) {
                val imported = Namer.createSpecialFunction(function)
                val result = JsScope.declareTemporaryName(function.suggestedName)
                result.imported = true
                result.specialFunction = function
                inlineFunctionContext!!.importBlock.statements.add(JsAstUtils.newVar(result, imported))
                result
            }
        }
    }

    fun getVariableForPropertyMetadata(property: VariableDescriptorWithAccessors): JsName {
        return staticContext.getVariableForPropertyMetadata(property)
    }

    companion object {

        fun rootContext(staticContext: StaticContext): TranslationContext {
            val rootDynamicContext = DynamicContext.rootContext(
                staticContext.fragment.scope, staticContext.fragment.initializerBlock
            )
            val rootAliasingContext = AliasingContext.cleanContext
            return TranslationContext(null, staticContext, rootDynamicContext, rootAliasingContext, null, null)
        }

        private fun replaceModuleReference(
            expression: JsExpression,
            expectedModuleName: JsName,
            reexportExpr: JsExpression
        ): JsExpression {
            if (expression is JsNameRef) {
                if (expression.qualifier == null) {
                    return if (expectedModuleName === expression.name) reexportExpr else expression
                } else {
                    val newQualifier = replaceModuleReference(expression.qualifier!!, expectedModuleName, reexportExpr)
                    if (newQualifier === expression.qualifier) {
                        return expression
                    }
                    val result = if (expression.name != null)
                        JsNameRef(expression.name!!, newQualifier)
                    else
                        JsNameRef(expression.ident, newQualifier)
                    result.copyMetadataFrom(expression)
                    return result
                }
            } else {
                return expression
            }
        }
    }
}
