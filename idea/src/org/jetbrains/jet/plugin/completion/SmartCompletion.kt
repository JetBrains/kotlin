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
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPropertyDescriptor
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.editor.EditorModificationUtil
import org.jetbrains.jet.plugin.codeInsight.ImplementMethodsHandler
import com.intellij.psi.PsiDocumentManager
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences

trait SmartCompletionData{
    fun accepts(descriptor: DeclarationDescriptor): Boolean
    val additionalElements: Iterable<LookupElement>
}

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
    val expectedType: JetType? = bindingContext.get(BindingContext.EXPECTED_EXPRESSION_TYPE, expressionWithType)
    if (expectedType == null || expectedType.isError()) return null

    val itemsToSkip = calcItemsToSkip(expressionWithType, resolveSession)

    val additionalElements = ArrayList<LookupElement>()

    if (receiver == null) {
        typeInstantiationItems(expectedType, resolveSession, bindingContext).toCollection(additionalElements)
        thisItems(expressionWithType, expectedType, bindingContext).toCollection(additionalElements)
        staticMembers(expressionWithType, expectedType, resolveSession, bindingContext).toCollection(additionalElements)
    }

    val dataFlowInfo = bindingContext.get(BindingContext.EXPRESSION_DATA_FLOW_INFO, expressionWithType)
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
        override fun accepts(descriptor: DeclarationDescriptor)
                = !itemsToSkip.contains(descriptor) && typesOf(descriptor).any { isSubtypeOf(it, expectedType) }

        override val additionalElements = additionalElements
    }
}

private fun calcItemsToSkip(expression: JetExpression, resolveSession: ResolveSessionForBodies): Collection<DeclarationDescriptor> {
    val parent = expression.getParent()
    when(parent) {
        is JetProperty -> {
            //TODO: this can be filtered out by ordinary completion
            if (expression == parent.getInitializer()) {
                return resolveSession.resolveToElement(parent).get(BindingContext.DECLARATION_TO_DESCRIPTOR, parent).toList()
            }
        }

        is JetBinaryExpression -> {
            if (parent.getRight() == expression && parent.getOperationToken() == JetTokens.EQ) {
                val left = parent.getLeft()
                if (left is JetReferenceExpression) {
                    return resolveSession.resolveToElement(left).get(BindingContext.REFERENCE_TARGET, left).toList()
                }
            }
        }
    }
    return listOf()
}

private fun typeInstantiationItems(expectedType: JetType, resolveSession: ResolveSessionForBodies, bindingContext: BindingContext): Iterable<LookupElement> {
    val typeConstructor: TypeConstructor = expectedType.getConstructor()
    val classifier: ClassifierDescriptor? = typeConstructor.getDeclarationDescriptor()
    if (!(classifier is ClassDescriptor)) return listOf()

    //TODO: check for constructor's visibility

    val lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, classifier)

    var lookupString = lookupElement.getLookupString()

    val typeArgs = expectedType.getArguments()
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

    if (suppressAutoInsertion) {
        return listOf(AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(lookupElementDecorated))
    }
    else{
        return listOf(lookupElementDecorated)
    }
}

private fun thisItems(context: JetExpression, expectedType: JetType, bindingContext: BindingContext): Iterable<LookupElement> {
    val scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, context)
    if (scope == null) return listOf()

    val receivers: List<ReceiverParameterDescriptor> = scope.getImplicitReceiversHierarchy()
    val result = ArrayList<LookupElement>()
    for (i in 0..receivers.size - 1) {
        val receiver = receivers[i]
        val thisType = receiver.getType()
        if (isSubtypeOf(thisType, expectedType)) {
            //TODO: use this code when KT-4258 fixed
            //val expressionText = if (i == 0) "this" else "this@" + (thisQualifierName(receiver, bindingContext) ?: continue)
            val qualifier = if (i == 0) null else thisQualifierName(receiver, bindingContext) ?: continue
            val expressionText = if (qualifier == null) "this" else "this@" + qualifier
            result.add(LookupElementBuilder.create(expressionText).withTypeText(DescriptorRenderer.TEXT.renderType(thisType)))
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
            val receiverType = bindingContext.get(BindingContext.EXPRESSION_TYPE, receiver)
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
        val notNullVariables = nullabilityInfo.iterator()
                .filter { it.getValue() == Nullability.NOT_NULL }
                .map { dataFlowValueToVariable(it.getKey()) }
                .filterNotNullTo(HashSet<VariableDescriptor>())

        return ProcessDataFlowInfoResult(variableToType, notNullVariables)
    }

    return ProcessDataFlowInfoResult()
}

// adds java static members, enum members and members from class object
private fun staticMembers(context: JetExpression, expectedType: JetType, resolveSession: ResolveSessionForBodies, bindingContext: BindingContext): Iterable<LookupElement> {
    val classDescriptor = TypeUtils.getClassDescriptor(expectedType)
    if (classDescriptor == null) return listOf()
    if (classDescriptor.getName().isSpecial()) return listOf()
    val scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, context)
    if (scope == null) return listOf()

    val descriptors = ArrayList<DeclarationDescriptor>()

    val isSuitableCallable: (DeclarationDescriptor) -> Boolean = {
        it is CallableDescriptor && it.getReturnType()?.let { isSubtypeOf(it, expectedType) } ?: false
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

        return object: LookupElementDecorator<LookupElement>(lookupElement){
            override fun getLookupString() = lookupString

            override fun renderElement(presentation: LookupElementPresentation) {
                lookupElement.renderElement(presentation)

                presentation.setItemText(qualifierPresentation + "." + presentation.getItemText())

                val tailText = " (" + DescriptorUtils.getFqName(classDescriptor.getContainingDeclaration()) + ")"
                if (descriptor is FunctionDescriptor) {
                    presentation.appendTailText(tailText, true)
                }
                else{
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
    }

    return descriptors
            .filter { !(it is DeclarationDescriptorWithVisibility) || Visibilities.isVisible(it, scope.getContainingDeclaration()) }
            .map({toLookupElement(it)}) //TODO: use ::toLookupElement but it causes compiler to crash
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