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

trait SmartCompletionData{
    fun toElement(descriptor: DeclarationDescriptor): LookupElement?
    val additionalElements: Iterable<LookupElement>
}

enum class Tail {
  COMMA
  PARENTHESIS
}

data class ExpectedTypeInfo(val `type`: JetType, val tail: Tail?)

fun buildSmartCompletionData(expression: JetSimpleNameExpression, resolveSession: ResolveSessionForBodies): SmartCompletionData? {
    val parent = expression.getParent()
    val expressionWithType: JetExpression;
    val receiver: JetExpression?
    if (parent is JetQualifiedExpression) {
        expressionWithType = parent
        receiver = parent.getReceiverExpression()
    }
    else {
        expressionWithType = expression
        receiver = null
    }

    val bindingContext = resolveSession.resolveToElement(expressionWithType)

    val allExpectedTypes = calcExpectedTypes(expressionWithType, bindingContext, resolveSession.getModuleDescriptor()) ?: return null
    val expectedTypes = allExpectedTypes.filter { !it.`type`.isError() }
    if (expectedTypes.isEmpty()) return null

    val itemsToSkip = calcItemsToSkip(expressionWithType, resolveSession)

    val additionalElements = ArrayList<LookupElement>()

    if (receiver == null) {
        for (expectedType in expectedTypes) {
            //TODO: there can be duplicates here for multiple expected types
            typeInstantiationItems(expectedType, resolveSession, bindingContext).toCollection(additionalElements)

            //TODO: there can be duplicates here for multiple expected types
            staticMembers(expressionWithType, expectedType, resolveSession, bindingContext).toCollection(additionalElements)
        }

        thisItems(expressionWithType, expectedTypes, bindingContext).toCollection(additionalElements)
    }

    val dataFlowInfo = bindingContext[BindingContext.EXPRESSION_DATA_FLOW_INFO, expressionWithType]
    val (variableToTypes: Map<VariableDescriptor, Collection<JetType>>, notNullVariables: Set<VariableDescriptor>) = processDataFlowInfo(dataFlowInfo, receiver, bindingContext)

    fun typesOf(descriptor: DeclarationDescriptor): Iterable<JetType> {
        if (descriptor is CallableDescriptor) {
            var returnType = descriptor.getReturnType()
            if (returnType != null && KotlinBuiltIns.getInstance().isNothing(returnType!!)) { //TODO: maybe we should include them on the second press?
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

    return object: SmartCompletionData {
        override fun toElement(descriptor: DeclarationDescriptor): LookupElement? {
            if (itemsToSkip.contains(descriptor)) return null
            val matchedExpectedTypes = expectedTypes.filter { expectedType -> typesOf(descriptor).any { descriptorType -> isSubtypeOf(descriptorType, expectedType.`type`) } }
            if (matchedExpectedTypes.isEmpty()) return null
            val tail = mergeTails(matchedExpectedTypes.map { it.tail })
            return decorateLookupElement(DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, descriptor), tail)
        }

        override val additionalElements = additionalElements
    }
}

private fun calcExpectedTypes(expressionWithType: JetExpression, bindingContext: BindingContext, moduleDescriptor: ModuleDescriptor): Collection<ExpectedTypeInfo>? {
    val expectedTypes = calcArgumentExpectedTypes(expressionWithType, bindingContext, moduleDescriptor)
    if (expectedTypes != null) {
        return expectedTypes
    }
    else{
        val expectedType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, expressionWithType] ?: return null
        return listOf(ExpectedTypeInfo(expectedType, null))
    }
}

private fun calcArgumentExpectedTypes(expressionWithType: JetExpression, bindingContext: BindingContext, moduleDescriptor: ModuleDescriptor): Collection<ExpectedTypeInfo>? {
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

private fun calcItemsToSkip(expression: JetExpression, resolveSession: ResolveSessionForBodies): Collection<DeclarationDescriptor> {
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

private fun typeInstantiationItems(expectedType: ExpectedTypeInfo, resolveSession: ResolveSessionForBodies, bindingContext: BindingContext): Iterable<LookupElement> {
    val typeConstructor: TypeConstructor = expectedType.`type`.getConstructor()
    val classifier: ClassifierDescriptor? = typeConstructor.getDeclarationDescriptor()
    if (!(classifier is ClassDescriptor)) return listOf()

    //TODO: check for constructor's visibility

    val lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, classifier)

    var lookupString = lookupElement.getLookupString()

    val typeArgs = expectedType.`type`.getArguments()
    var itemText = lookupString + DescriptorRenderer.TEXT.renderTypeArguments(typeArgs)

    val insertHandler: InsertHandler<LookupElement>
    var suppressAutoInsertion: Boolean = false
    val typeText = DescriptorUtils.getFqName(classifier).toString() + DescriptorRenderer.SOURCE_CODE.renderTypeArguments(typeArgs)
    if (classifier.getModality() == Modality.ABSTRACT) {
        val constructorParenthesis = if (classifier.getKind() != ClassKind.TRAIT) "()" else ""
        itemText += constructorParenthesis
        itemText = "object: " + itemText + "{...}"
        lookupString = "object" //?
        insertHandler = InsertHandler<LookupElement> { (context, item) ->
            val editor = context.getEditor()
            val startOffset = context.getStartOffset()
            val text = "object: $typeText$constructorParenthesis {}"
            editor.getDocument().replaceString(startOffset, context.getTailOffset(), text)
            editor.getCaretModel().moveToOffset(startOffset + text.length - 1)

            PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
            val file = context.getFile() as JetFile
            val element = PsiTreeUtil.findElementOfClassAtRange(file, startOffset, startOffset + text.length, javaClass<JetElement>())
            if (element != null) {
                ShortenReferences.process(element)
            }

            ImplementMethodsHandler().invoke(context.getProject(), editor, context.getFile(), true)
        }
        suppressAutoInsertion = true
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
        insertHandler = InsertHandler<LookupElement> { (context, item) ->
            val editor = context.getEditor()
            val startOffset = context.getStartOffset()
            val text = typeText + "()"
            editor.getDocument().replaceString(startOffset, context.getTailOffset(), text)
            val endOffset = startOffset + text.length
            editor.getCaretModel().moveToOffset(if (caretPosition == CaretPosition.IN_BRACKETS) endOffset - 1 else endOffset)

            //TODO: autopopup parameter info and other functionality from JetFunctionInsertHandler

            PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
            val file = context.getFile() as JetFile
            val element = PsiTreeUtil.findElementOfClassAtRange(file, startOffset, endOffset, javaClass<JetElement>())
            if (element != null) {
                ShortenReferences.process(element)
            }
        }
    }

    val lookupElementDecorated = object: LookupElementDecorator<LookupElement>(lookupElement){
        override fun getLookupString() = lookupString

        override fun renderElement(presentation: LookupElementPresentation) {
            lookupElement.renderElement(presentation)
            presentation.setItemText(itemText)
        }

        override fun handleInsert(context: InsertionContext) {
            insertHandler.handleInsert(context, lookupElement)
        }
    }

    val lookupElementWithTail = decorateLookupElement(lookupElementDecorated, expectedType.tail)

    if (suppressAutoInsertion) {
        return listOf(AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(lookupElementWithTail))
    }
    else{
        return listOf(lookupElementWithTail)
    }
}

private fun thisItems(context: JetExpression, expectedTypes: Collection<ExpectedTypeInfo>, bindingContext: BindingContext): Iterable<LookupElement> {
    val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, context]
    if (scope == null) return listOf()

    val receivers: List<ReceiverParameterDescriptor> = scope.getImplicitReceiversHierarchy()
    val result = ArrayList<LookupElement>()
    for (i in 0..receivers.size - 1) {
        val receiver = receivers[i]
        val thisType = receiver.getType()
        val matchedExpectedTypes = expectedTypes.filter { isSubtypeOf(thisType, it.`type`) }
        if (matchedExpectedTypes.notEmpty) {
            //TODO: use this code when KT-4258 fixed
            //val expressionText = if (i == 0) "this" else "this@" + (thisQualifierName(receiver, bindingContext) ?: continue)
            val qualifier = if (i == 0) null else thisQualifierName(receiver, bindingContext) ?: continue
            val expressionText = if (qualifier == null) "this" else "this@" + qualifier
            val lookupElement = LookupElementBuilder.create(expressionText).withTypeText(DescriptorRenderer.TEXT.renderType(thisType))
            val tailType = mergeTails(matchedExpectedTypes.map { it.tail })
            result.add(decorateLookupElement(lookupElement, tailType))
        }
    }
    return result
}

private fun thisQualifierName(receiver: ReceiverParameterDescriptor, bindingContext: BindingContext): String? {
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

private data class ProcessDataFlowInfoResult(
        val variableToTypes: Map<VariableDescriptor, Collection<JetType>> = Collections.emptyMap(),
        val notNullVariables: Set<VariableDescriptor> = Collections.emptySet()
)

private fun processDataFlowInfo(dataFlowInfo: DataFlowInfo?, receiver: JetExpression?, bindingContext: BindingContext): ProcessDataFlowInfoResult {
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
private fun staticMembers(context: JetExpression, expectedType: ExpectedTypeInfo, resolveSession: ResolveSessionForBodies, bindingContext: BindingContext): Iterable<LookupElement> {
    val classDescriptor = TypeUtils.getClassDescriptor(expectedType.`type`)
    if (classDescriptor == null) return listOf()
    if (classDescriptor.getName().isSpecial()) return listOf()
    val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, context]
    if (scope == null) return listOf()

    val descriptors = ArrayList<DeclarationDescriptor>()

    val isSuitableCallable: (DeclarationDescriptor) -> Boolean = {
        it is CallableDescriptor && it.getReturnType()?.let { isSubtypeOf(it, expectedType.`type`) } ?: false
    }

    if (classDescriptor is JavaClassDescriptor) {
        val pseudoPackage = classDescriptor.getCorrespondingPackageFragment()
        if (pseudoPackage != null) {
            pseudoPackage.getMemberScope().getAllDescriptors().filterTo(descriptors, isSuitableCallable)
        }
    }

    val classObject = classDescriptor.getClassObjectDescriptor()
    if (classObject != null) {
        classObject.getDefaultType().getMemberScope().getAllDescriptors().filterTo(descriptors, isSuitableCallable)
    }

    if (classDescriptor.getKind() == ClassKind.ENUM_CLASS) {
        classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()
                .filterTo(descriptors) { it is ClassDescriptor && it.getKind() == ClassKind.ENUM_ENTRY }
    }

    fun toLookupElement(descriptor: DeclarationDescriptor): LookupElement {
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

        val insertHandler = InsertHandler<LookupElement> { (context, item) ->
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

            PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
            val file = context.getFile() as JetFile
            val element = PsiTreeUtil.findElementOfClassAtRange(file, startOffset, startOffset + qualifierText.length, javaClass<JetElement>())
            if (element != null) {
                ShortenReferences.process(element)
            }
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

        return decorateLookupElement(lookupElementDecorated, expectedType.tail)
    }

    return descriptors
            .filter { !(it is DeclarationDescriptorWithVisibility) || Visibilities.isVisible(it, scope.getContainingDeclaration()) }
            .map(::toLookupElement)
}

private fun mergeTails(tails: Collection<Tail?>): Tail? {
    if (tails.size == 1) return tails.single()
    return if (HashSet(tails).size == 1) tails.first() else null
}

private fun decorateLookupElement(lookupElement: LookupElement, tail: Tail?): LookupElement {
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

private fun isSubtypeOf(t: JetType, expectedType: JetType): Boolean{
    return !t.isError() && JetTypeChecker.INSTANCE.isSubtypeOf(t, expectedType)
}

private fun <T : Any> T?.toList(): List<T> = if (this != null) listOf(this) else listOf()

private fun <T> MutableCollection<T>.addAll(iterator: Iterator<T>) {
    for (item in iterator) {
        add(item)
    }
}

private fun String?.isNullOrEmpty() = this == null || this.isEmpty()
