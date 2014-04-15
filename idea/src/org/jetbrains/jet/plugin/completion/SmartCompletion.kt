package org.jetbrains.jet.plugin.completion

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.lang.types.*
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import com.intellij.codeInsight.lookup.*
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.codeInsight.completion.*
import org.jetbrains.jet.plugin.completion.handlers.*
import com.google.common.collect.SetMultimap
import java.util.*
import org.jetbrains.jet.lang.resolve.calls.autocasts.*
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.plugin.codeInsight.ImplementMethodsHandler
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.di.InjectorForMacros
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.jet.lang.resolve.calls.context.ContextDependency
import org.jetbrains.jet.lang.resolve.calls.context.CheckValueArgumentsMode
import org.jetbrains.jet.lang.resolve.calls.CompositeExtension
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import com.intellij.lang.ASTNode
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester
import org.jetbrains.jet.plugin.refactoring.JetNameValidator
import com.intellij.openapi.project.Project

class SmartCompletion(val expression: JetSimpleNameExpression,
                      val resolveSession: ResolveSessionForBodies,
                      val visibilityFilter: (DeclarationDescriptor) -> Boolean) {

    private val bindingContext: BindingContext
    private val moduleDescriptor: ModuleDescriptor
    private val project: Project

    {
        this.bindingContext = resolveSession.resolveToElement(expression)
        this.moduleDescriptor = resolveSession.getModuleDescriptor()
        this.project = expression.getProject()
    }

    private enum class Tail {
        COMMA
        PARENTHESIS
    }

    private data class ExpectedTypeInfo(val `type`: JetType, val tail: Tail?)

    public fun buildLookupElements(referenceVariants: Iterable<DeclarationDescriptor>): Collection<LookupElement>? {
        val parent = expression.getParent()
        val expressionWithType: JetExpression
        val receiver: JetExpression?
        if (parent is JetQualifiedExpression) {
            expressionWithType = parent
            receiver = parent.getReceiverExpression()
        }
        else {
            expressionWithType = expression
            receiver = null
        }

        val allExpectedTypes = calcExpectedTypes(expressionWithType) ?: return null
        val expectedTypes = allExpectedTypes.filter { !it.`type`.isError() }
        if (expectedTypes.isEmpty()) return null

        val result = ArrayList<LookupElement>()

        val typesOf: (DeclarationDescriptor) -> Iterable<JetType> = dataFlowToDescriptorTypes(expressionWithType, receiver)

        val itemsToSkip = calcItemsToSkip(expressionWithType)

        val functionExpectedTypes = expectedTypes.filter { KotlinBuiltIns.getInstance().isExactFunctionOrExtensionFunctionType(it.`type`) }

        for (descriptor in referenceVariants) {
            if (itemsToSkip.contains(descriptor)) continue

            val matchedExpectedTypes = expectedTypes.filter { expectedType ->
                typesOf(descriptor).any { descriptorType -> isSubtypeOf(descriptorType, expectedType.`type`) }
            }
            if (matchedExpectedTypes.isNotEmpty()) {
                val lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, descriptor)
                result.add(addTailToLookupElement(lookupElement, matchedExpectedTypes))
            }

            if (receiver == null) {
                toFunctionReferenceLookupElement(descriptor, functionExpectedTypes)?.let { result.add(it) }
            }
        }

        if (receiver == null) {
            result.addTypeInstantiationItems(expectedTypes)

            result.addStaticMembers(expressionWithType, expectedTypes)

            result.addThisItems(expressionWithType, expectedTypes)

            result.addLambdaItems(functionExpectedTypes)
        }

        return result
    }

    private fun calcExpectedTypes(expressionWithType: JetExpression): Collection<ExpectedTypeInfo>? {
        val expectedTypes = calcArgumentExpectedTypes(expressionWithType)
        if (expectedTypes != null) {
            return expectedTypes
        }
        else {
            val expectedType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, expressionWithType] ?: return null
            return listOf(ExpectedTypeInfo(expectedType, null))
        }
    }

    private fun calcArgumentExpectedTypes(expressionWithType: JetExpression): Collection<ExpectedTypeInfo>? {
        val argument = expressionWithType.getParent() as? JetValueArgument ?: return null
        if (argument.isNamed()) return null //TODO - support named arguments (also do not forget to check for presence of named arguments before)
        val argumentList = argument.getParent() as JetValueArgumentList
        val argumentIndex = argumentList.getArguments().indexOf(argument)
        val callExpression = argumentList.getParent() as? JetCallExpression ?: return null
        val calleeExpression = callExpression.getCalleeExpression()

        val parent = callExpression.getParent()
        val receiver: ReceiverValue
        val callOperationNode: ASTNode?
        if (parent is JetQualifiedExpression && callExpression == parent.getSelectorExpression()) {
            val receiverExpression = parent.getReceiverExpression()
            val expressionType = bindingContext[BindingContext.EXPRESSION_TYPE, receiverExpression] ?: return null
            receiver = ExpressionReceiver(receiverExpression, expressionType)
            callOperationNode = parent.getOperationTokenNode()
        }
        else {
            receiver = ReceiverValue.NO_RECEIVER
            callOperationNode = null
        }
        val call = CallMaker.makeCall(receiver, callOperationNode, callExpression)

        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, calleeExpression] ?: return null //TODO: discuss it

        val callResolutionContext = BasicCallResolutionContext.create(
                DelegatingBindingTrace(bindingContext, "Temporary trace for smart completion"),
                resolutionScope,
                call,
                bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, callExpression] ?: TypeUtils.NO_EXPECTED_TYPE,
                bindingContext[BindingContext.EXPRESSION_DATA_FLOW_INFO, callExpression] ?: DataFlowInfo.EMPTY,
                ContextDependency.INDEPENDENT,
                CheckValueArgumentsMode.ENABLED,
                CompositeExtension(listOf()),
                false).replaceCollectAllCandidates(true)
        val callResolver = InjectorForMacros(expressionWithType.getProject(), moduleDescriptor).getCallResolver()!!
        val results: OverloadResolutionResults<FunctionDescriptor> = callResolver.resolveFunctionCall(callResolutionContext)

        val expectedTypes = HashSet<ExpectedTypeInfo>()
        for (candidate: ResolvedCall<FunctionDescriptor> in results.getAllCandidates()!!) {
            val parameters = candidate.getResultingDescriptor().getValueParameters()
            if (parameters.size <= argumentIndex) continue
            val parameterDescriptor = parameters[argumentIndex]
            val tail = if (argumentIndex == parameters.size - 1) Tail.PARENTHESIS else Tail.COMMA
            expectedTypes.add(ExpectedTypeInfo(parameterDescriptor.getType(), tail))
        }
        return expectedTypes
    }

    private fun calcItemsToSkip(expression: JetExpression): Collection<DeclarationDescriptor> {
        val parent = expression.getParent()
        when(parent) {
            is JetProperty -> {
                //TODO: this can be filtered out by ordinary completion
                if (expression == parent.getInitializer()) {
                    return resolveSession.resolveToElement(parent)[BindingContext.DECLARATION_TO_DESCRIPTOR, parent].toList()
                }
            }

            is JetBinaryExpression -> {
                if (parent.getRight() == expression && parent.getOperationToken() == JetTokens.EQ) {
                    val left = parent.getLeft()
                    if (left is JetReferenceExpression) {
                        return resolveSession.resolveToElement(left)[BindingContext.REFERENCE_TARGET, left].toList()
                    }
                }
            }
        }
        return listOf()
    }

    private fun toFunctionReferenceLookupElement(descriptor: DeclarationDescriptor,
                                                 functionExpectedTypes: Collection<ExpectedTypeInfo>): LookupElement? {
        if (functionExpectedTypes.isEmpty()) return null

        fun toLookupElement(descriptor: FunctionDescriptor): LookupElement? {
            val functionType = functionType(descriptor)
            if (functionType == null) return null

            val matchedExpectedTypes = functionExpectedTypes.filter { isSubtypeOf(functionType, it.`type`) }
            if (matchedExpectedTypes.isEmpty()) return null

            val lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, descriptor)
            val text = "::" + (if (descriptor is ConstructorDescriptor) descriptor.getContainingDeclaration().getName() else descriptor.getName())
            val lookupElementDecorated = object: LookupElementDecorator<LookupElement>(lookupElement) {
                override fun getLookupString() = text

                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    presentation.setItemText(text)
                    presentation.setTypeText("")
                }

                override fun handleInsert(context: InsertionContext) {
                }
            }

            return addTailToLookupElement(lookupElementDecorated, matchedExpectedTypes)
        }

        if (descriptor is SimpleFunctionDescriptor) {
            return toLookupElement(descriptor)
        }
        else if (descriptor is ClassDescriptor && descriptor.getModality() != Modality.ABSTRACT) {
            val constructors = descriptor.getConstructors().filter(visibilityFilter)
            if (constructors.size == 1) {
                //TODO: this code is to be changed if overloads to start work after ::
                return toLookupElement(constructors.single())
            }
        }

        return null
    }

    private fun MutableCollection<LookupElement>.addTypeInstantiationItems(expectedTypes: Collection<ExpectedTypeInfo>) {
        val expectedTypesGrouped: Map<JetType, List<ExpectedTypeInfo>> = expectedTypes.groupBy { TypeUtils.makeNotNullable(it.`type`) }
        for ((jetType, types) in expectedTypesGrouped) {
            val tail = mergeTails(types.map { it.tail })
            addTypeInstantiationItems(jetType, tail)
        }
    }

    private fun MutableCollection<LookupElement>.addTypeInstantiationItems(jetType: JetType, tail: Tail?) {
        if (KotlinBuiltIns.getInstance().isExactFunctionOrExtensionFunctionType(jetType)) return // do not show "object: ..." for function types

        val classifier = jetType.getConstructor().getDeclarationDescriptor()
        if (!(classifier is ClassDescriptor)) return
        //TODO: check for constructor's visibility

        var lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, classifier)

        var lookupString = lookupElement.getLookupString()

        val typeArgs = jetType.getArguments()
        var itemText = lookupString + DescriptorRenderer.TEXT.renderTypeArguments(typeArgs)

        val insertHandler: InsertHandler<LookupElement>
        val typeText = DescriptorUtils.getFqName(classifier).toString() + DescriptorRenderer.SOURCE_CODE.renderTypeArguments(typeArgs)
        if (classifier.getModality() == Modality.ABSTRACT) {
            val constructorParenthesis = if (classifier.getKind() != ClassKind.TRAIT) "()" else ""
            itemText += constructorParenthesis
            itemText = "object: " + itemText + "{...}"
            lookupString = "object" //?
            insertHandler = InsertHandler<LookupElement> {(context, item) ->
                val editor = context.getEditor()
                val startOffset = context.getStartOffset()
                val text = "object: $typeText$constructorParenthesis {}"
                editor.getDocument().replaceString(startOffset, context.getTailOffset(), text)
                editor.getCaretModel().moveToOffset(startOffset + text.length - 1)

                shortenReferences(context, startOffset, startOffset + text.length)

                ImplementMethodsHandler().invoke(context.getProject(), editor, context.getFile(), true)
            }
            lookupElement = suppressAutoInsertion(lookupElement)
        }
        else {
            itemText += "()"
            val constructors: Collection<ConstructorDescriptor> = classifier.getConstructors()
            val caretPosition =
                    if (constructors.size == 0)
                        CaretPosition.AFTER_BRACKETS
                    else if (constructors.size == 1)
                        if (constructors.first().getValueParameters().isEmpty()) CaretPosition.AFTER_BRACKETS else CaretPosition.IN_BRACKETS
                    else
                        CaretPosition.IN_BRACKETS
            insertHandler = InsertHandler<LookupElement> {(context, item) ->
                val editor = context.getEditor()
                val startOffset = context.getStartOffset()
                val text = typeText + "()"
                editor.getDocument().replaceString(startOffset, context.getTailOffset(), text)
                val endOffset = startOffset + text.length
                editor.getCaretModel().moveToOffset(if (caretPosition == CaretPosition.IN_BRACKETS) endOffset - 1 else endOffset)

                //TODO: autopopup parameter info and other functionality from JetFunctionInsertHandler

                shortenReferences(context, startOffset, endOffset)
            }
        }

        val lookupElementDecorated = object: LookupElementDecorator<LookupElement>(lookupElement) {
            override fun getLookupString() = lookupString

            override fun renderElement(presentation: LookupElementPresentation) {
                lookupElement.renderElement(presentation)
                presentation.setItemText(itemText)
            }

            override fun handleInsert(context: InsertionContext) {
                insertHandler.handleInsert(context, lookupElement)
            }
        }

        add(addTailToLookupElement(lookupElementDecorated, tail))
    }

    private fun MutableCollection<LookupElement>.addThisItems(context: JetExpression, expectedTypes: Collection<ExpectedTypeInfo>) {
        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, context]
        if (scope == null) return

        val receivers: List<ReceiverParameterDescriptor> = scope.getImplicitReceiversHierarchy()
        for (i in 0..receivers.size - 1) {
            val receiver = receivers[i]
            val thisType = receiver.getType()
            val matchedExpectedTypes = expectedTypes.filter { isSubtypeOf(thisType, it.`type`) }
            if (matchedExpectedTypes.notEmpty) {
                //TODO: use this code when KT-4258 fixed
                //val expressionText = if (i == 0) "this" else "this@" + (thisQualifierName(receiver, bindingContext) ?: continue)
                val qualifier = if (i == 0) null else thisQualifierName(receiver) ?: continue
                val expressionText = if (qualifier == null) "this" else "this@" + qualifier
                val lookupElement = LookupElementBuilder.create(expressionText).withTypeText(DescriptorRenderer.TEXT.renderType(thisType))
                add(addTailToLookupElement(lookupElement, matchedExpectedTypes))
            }
        }
    }

    private fun thisQualifierName(receiver: ReceiverParameterDescriptor): String? {
        val descriptor: DeclarationDescriptor = receiver.getContainingDeclaration()
        val name: Name = descriptor.getName()
        if (!name.isSpecial()) return name.asString()

        val psiElement = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor)
        val expression: JetExpression? = when (psiElement) {
            is JetFunctionLiteral -> psiElement.getParent() as? JetFunctionLiteralExpression
            is JetObjectDeclaration -> psiElement.getParent() as? JetObjectLiteralExpression
            else -> null
        }
        return ((((expression?.getParent() as? JetValueArgument)
                   ?.getParent() as? JetValueArgumentList)
                       ?.getParent() as? JetCallExpression)
                           ?.getCalleeExpression() as? JetSimpleNameExpression)
                               ?.getReferencedName()
    }


    private fun MutableCollection<LookupElement>.addLambdaItems(functionExpectedTypes: Collection<ExpectedTypeInfo>) {
        val distinctTypes = functionExpectedTypes.map { it.`type` }.toSet()

        fun createLookupElement(lookupString: String, textBeforeCaret: String, textAfterCaret: String, shortenRefs: Boolean): LookupElement {
            val lookupElement = LookupElementBuilder.create(lookupString).withInsertHandler {(context, lookupElement) ->
                val offset = context.getEditor().getCaretModel().getOffset()
                val startOffset = offset - lookupString.length
                context.getDocument().deleteString(startOffset, offset) // delete inserted lookup string
                context.getDocument().insertString(startOffset, textBeforeCaret + textAfterCaret)
                context.getEditor().getCaretModel().moveToOffset(startOffset + textBeforeCaret.length)

                if (shortenRefs) {
                    shortenReferences(context, startOffset, startOffset + textBeforeCaret.length + textAfterCaret.length)
                }
            }
            return suppressAutoInsertion(lookupElement)
        }

        val singleType = if (distinctTypes.size == 1) distinctTypes.single() else null
        val singleSignatureLength = singleType?.let { KotlinBuiltIns.getInstance().getParameterTypeProjectionsFromFunctionType(it).size }
        val offerNoParametersLambda = singleSignatureLength == 0 || singleSignatureLength == 1
        if (offerNoParametersLambda) {
            val lookupElement = createLookupElement("{...}", "{ ", " }", shortenRefs = false)
            add(addTailToLookupElement(lookupElement, functionExpectedTypes))
        }

        if (singleSignatureLength != 0) {
            fun functionParameterTypes(functionType: JetType)
                    = KotlinBuiltIns.getInstance().getParameterTypeProjectionsFromFunctionType(functionType).map { it.getType() }

            for (functionType in distinctTypes) {
                val parameterTypes = functionParameterTypes(functionType)
                val parametersPresentation = parameterTypes.map { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it) }.makeString(", ")

                val useExplicitTypes = distinctTypes.stream().any { it != functionType && functionParameterTypes(it).size == parameterTypes.size }
                val nameValidator = JetNameValidator.getEmptyValidator(project)

                fun parameterName(parameterType: JetType) = JetNameSuggester.suggestNames(parameterType, nameValidator, "p")[0]

                fun parameterText(parameterType: JetType): String {
                    return if (useExplicitTypes)
                        parameterName(parameterType) + ": " + DescriptorRenderer.SOURCE_CODE.renderType(parameterType)
                    else
                        parameterName(parameterType)
                }

                val parametersText = parameterTypes.map(::parameterText).makeString(", ")

                val useParenthesis = parameterTypes.size != 1
                fun wrap(s: String) = if (useParenthesis) "($s)" else s

                val lookupString = "{ ${wrap(parametersPresentation)} -> ... }"
                val lookupElement = createLookupElement(lookupString, "{ ${wrap(parametersText)} -> ", " }", shortenRefs = true)
                add(addTailToLookupElement(lookupElement, functionExpectedTypes.filter { it.`type` == functionType }))
            }
        }
    }

    private fun dataFlowToDescriptorTypes(expression: JetExpression, receiver: JetExpression?): (DeclarationDescriptor) -> Iterable<JetType> {
        val dataFlowInfo = bindingContext[BindingContext.EXPRESSION_DATA_FLOW_INFO, expression]
        val (variableToTypes: Map<VariableDescriptor, Collection<JetType>>, notNullVariables: Set<VariableDescriptor>)
            = processDataFlowInfo(dataFlowInfo, receiver)

        fun typesOf(descriptor: DeclarationDescriptor): Iterable<JetType> {
            if (descriptor is CallableDescriptor) {
                var returnType = descriptor.getReturnType()
                if (returnType != null && KotlinBuiltIns.getInstance().isNothing(returnType!!)) {
                    //TODO: maybe we should include them on the second press?
                    return listOf()
                }
                if (descriptor is VariableDescriptor) {
                    if (notNullVariables.contains(descriptor) && returnType != null) {
                        returnType = TypeUtils.makeNotNullable(returnType!!)
                    }

                    val autoCastTypes = variableToTypes[descriptor]
                    if (autoCastTypes != null && !autoCastTypes.isEmpty()) {
                        return autoCastTypes + returnType.toList()
                    }
                }
                return returnType.toList()
            }
            else if (descriptor is ClassDescriptor && descriptor.getKind() == ClassKind.ENUM_ENTRY) {
                return listOf(descriptor.getDefaultType())
            }
            else {
                return listOf()
            }
        }

        return ::typesOf
    }

    private data class ProcessDataFlowInfoResult(
            val variableToTypes: Map<VariableDescriptor, Collection<JetType>> = Collections.emptyMap(),
            val notNullVariables: Set<VariableDescriptor> = Collections.emptySet()
    )

    private fun processDataFlowInfo(dataFlowInfo: DataFlowInfo?, receiver: JetExpression?): ProcessDataFlowInfoResult {
        if (dataFlowInfo != null) {
            val dataFlowValueToVariable: (DataFlowValue) -> VariableDescriptor?
            if (receiver != null) {
                val receiverType = bindingContext[BindingContext.EXPRESSION_TYPE, receiver]
                if (receiverType != null) {
                    val receiverId = DataFlowValueFactory.createDataFlowValue(receiver, receiverType, bindingContext).getId()
                    dataFlowValueToVariable = {(value) ->
                        val id = value.getId()
                        if (id is com.intellij.openapi.util.Pair<*, *> && id.first == receiverId) id.second as? VariableDescriptor else null
                    }
                }
                else {
                    return ProcessDataFlowInfoResult()
                }
            }
            else {
                dataFlowValueToVariable = {(value) -> value.getId() as? VariableDescriptor }
            }

            val variableToType = HashMap<VariableDescriptor, Collection<JetType>>()
            val typeInfo: SetMultimap<DataFlowValue, JetType> = dataFlowInfo.getCompleteTypeInfo()
            for ((dataFlowValue, types) in typeInfo.asMap().entrySet()) {
                val variable = dataFlowValueToVariable.invoke(dataFlowValue)
                if (variable != null) {
                    variableToType[variable] = types
                }
            }

            val nullabilityInfo: Map<DataFlowValue, Nullability> = dataFlowInfo.getCompleteNullabilityInfo()
            val notNullVariables = nullabilityInfo
                    .filter { it.getValue() == Nullability.NOT_NULL }
                    .map { dataFlowValueToVariable(it.getKey()) }
                    .filterNotNullTo(HashSet<VariableDescriptor>())

            return ProcessDataFlowInfoResult(variableToType, notNullVariables)
        }

        return ProcessDataFlowInfoResult()
    }

    // adds java static members, enum members and members from class object
    private fun MutableCollection<LookupElement>.addStaticMembers(context: JetExpression, expectedTypes: Collection<ExpectedTypeInfo>) {
        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, context]
        if (scope == null) return

        val expectedTypesByClass = expectedTypes.groupBy { TypeUtils.getClassDescriptor(it.`type`) }
        for ((classDescriptor, expectedTypesForClass) in expectedTypesByClass) {
            if (classDescriptor != null && !classDescriptor.getName().isSpecial()) {
                addStaticMembers(classDescriptor, expectedTypesForClass, scope)
            }
        }
    }

    private fun MutableCollection<LookupElement>.addStaticMembers(classDescriptor: ClassDescriptor,
                                                                  expectedTypes: Collection<ExpectedTypeInfo>,
                                                                  scope: JetScope) {

        val memberDescriptors = HashMap<DeclarationDescriptor, MutableList<ExpectedTypeInfo>>()

        for (expectedType in expectedTypes) {
            fun addMemberDescriptor(descriptor: DeclarationDescriptor) {
                val list = memberDescriptors[descriptor]
                if (list != null) {
                    list.add(expectedType)
                }
                else {
                    if (descriptor is DeclarationDescriptorWithVisibility && !Visibilities.isVisible(descriptor, scope.getContainingDeclaration())) return

                    val newList = ArrayList<ExpectedTypeInfo>()
                    newList.add(expectedType)
                    memberDescriptors[descriptor] = newList
                }
            }

            fun isSuitableCallable(descriptor: DeclarationDescriptor)
                    = descriptor is CallableDescriptor && descriptor.getReturnType()?.let { isSubtypeOf(it, expectedType.`type`) } ?: false

            if (classDescriptor is JavaClassDescriptor) {
                val pseudoPackage = classDescriptor.getCorrespondingPackageFragment()
                if (pseudoPackage != null) {
                    pseudoPackage.getMemberScope().getAllDescriptors().filter(::isSuitableCallable).forEach(::addMemberDescriptor)
                }
            }

            val classObject = classDescriptor.getClassObjectDescriptor()
            if (classObject != null) {
                classObject.getDefaultType().getMemberScope().getAllDescriptors().filter(::isSuitableCallable).forEach(::addMemberDescriptor)
            }

            if (classDescriptor.getKind() == ClassKind.ENUM_CLASS) {
                classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()
                        .filter { it is ClassDescriptor && it.getKind() == ClassKind.ENUM_ENTRY }.forEach(::addMemberDescriptor)
            }
        }

        for ((descriptor, descriptorExpectedTypes) in memberDescriptors) {
            val lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, descriptor)
            val qualifierPresentation = classDescriptor.getName().asString()
            val lookupString = qualifierPresentation + "." + lookupElement.getLookupString()
            val qualifierText = DescriptorUtils.getFqName(classDescriptor).asString() //TODO: escape keywords

            val caretPosition: CaretPosition?
            if (descriptor is FunctionDescriptor) {
                caretPosition = if (descriptor.getValueParameters().empty) CaretPosition.AFTER_BRACKETS else CaretPosition.IN_BRACKETS
            }
            else {
                caretPosition = null
            }

            val insertHandler = InsertHandler<LookupElement> {(context, item) ->
                val editor = context.getEditor()
                val startOffset = context.getStartOffset()
                var text = qualifierText + "." + descriptor.getName().asString() //TODO: escape
                if (descriptor is FunctionDescriptor) {
                    text += "()"
                    //TODO: autopopup parameter info and other functionality from JetFunctionInsertHandler
                }

                editor.getDocument().replaceString(startOffset, context.getTailOffset(), text)
                val endOffset = startOffset + text.length
                editor.getCaretModel().moveToOffset(if (caretPosition == CaretPosition.IN_BRACKETS) endOffset - 1 else endOffset)

                shortenReferences(context, startOffset, startOffset + qualifierText.length)
            }

            val lookupElementDecorated = object: LookupElementDecorator<LookupElement>(lookupElement) {
                override fun getLookupString() = lookupString

                override fun renderElement(presentation: LookupElementPresentation) {
                    lookupElement.renderElement(presentation)

                    presentation.setItemText(qualifierPresentation + "." + presentation.getItemText())

                    val tailText = " (" + DescriptorUtils.getFqName(classDescriptor.getContainingDeclaration()) + ")"
                    if (descriptor is FunctionDescriptor) {
                        presentation.appendTailText(tailText, true)
                    }
                    else {
                        presentation.setTailText(tailText, true)
                    }

                    if (presentation.getTypeText().isNullOrEmpty()) {
                        presentation.setTypeText(DescriptorRenderer.TEXT.renderType(classDescriptor.getDefaultType()))
                    }
                }

                override fun handleInsert(context: InsertionContext) {
                    insertHandler.handleInsert(context, lookupElement)
                }
            }

            add(addTailToLookupElement(lookupElementDecorated, descriptorExpectedTypes))
        }
    }

    private fun shortenReferences(context: InsertionContext, startOffset: Int, endOffset: Int) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
        val file = context.getFile() as JetFile
        val element = PsiTreeUtil.findElementOfClassAtRange(file, startOffset, endOffset, javaClass<JetElement>())
        if (element != null) {
            ShortenReferences.process(element)
        }
    }

    private fun mergeTails(tails: Collection<Tail?>): Tail? {
        if (tails.size == 1) return tails.single()
        return if (HashSet(tails).size == 1) tails.first() else null
    }

    private fun addTailToLookupElement(lookupElement: LookupElement, tail: Tail?): LookupElement {
        return when (tail) {
            null -> lookupElement

            Tail.COMMA -> object: LookupElementDecorator<LookupElement>(lookupElement) {
                override fun handleInsert(context: InsertionContext) {
                    WithTailInsertHandler(',', true /*TODO: use code style option*/).handleInsert(context, lookupElement)
                }
            }

            Tail.PARENTHESIS -> object: LookupElementDecorator<LookupElement>(lookupElement) {
                override fun handleInsert(context: InsertionContext) {
                    WithTailInsertHandler(')', false).handleInsert(context, lookupElement)
                }
            }
        }
    }

    private fun addTailToLookupElement(lookupElement: LookupElement, matchedExpectedTypes: Collection<ExpectedTypeInfo>): LookupElement
            = addTailToLookupElement(lookupElement, mergeTails(matchedExpectedTypes.map { it.tail }))

    private fun suppressAutoInsertion(lookupElement: LookupElement) = AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(lookupElement)

    private fun functionType(function: FunctionDescriptor): JetType? {
        return KotlinBuiltIns.getInstance().getKFunctionType(function.getAnnotations(),
                                                             null,
                                                             function.getValueParameters().map { it.getType() },
                                                             function.getReturnType() ?: return null,
                                                             function.getReceiverParameter() != null)
    }

    private fun isSubtypeOf(t: JetType, expectedType: JetType) = !t.isError() && JetTypeChecker.INSTANCE.isSubtypeOf(t, expectedType)

    private fun <T : Any> T?.toList(): List<T> = if (this != null) listOf(this) else listOf()

    private fun String?.isNullOrEmpty() = this == null || this.isEmpty()
}