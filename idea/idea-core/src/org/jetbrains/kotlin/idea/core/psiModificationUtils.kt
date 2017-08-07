/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getLambdaArgumentName
import org.jetbrains.kotlin.psi.psiUtil.hasBody
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentsInParentheses
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError

@Suppress("UNCHECKED_CAST")
inline fun <reified T : PsiElement> PsiElement.replaced(newElement: T): T {
    val result = replace(newElement)
    return if (result is T)
        result
    else
        (result as KtParenthesizedExpression).expression as T
}

@Suppress("UNCHECKED_CAST")
fun <T : PsiElement> T.copied(): T = copy() as T

fun KtLambdaArgument.moveInsideParentheses(bindingContext: BindingContext): KtCallExpression {
    return moveInsideParenthesesAndReplaceWith(this.getArgumentExpression(), bindingContext)
}

fun KtLambdaArgument.moveInsideParenthesesAndReplaceWith(
        replacement: KtExpression,
        bindingContext: BindingContext
): KtCallExpression = moveInsideParenthesesAndReplaceWith(replacement, getLambdaArgumentName(bindingContext))

fun KtLambdaArgument.moveInsideParenthesesAndReplaceWith(
        replacement: KtExpression,
        functionLiteralArgumentName: Name?
): KtCallExpression {
    val oldCallExpression = parent as KtCallExpression
    val newCallExpression = oldCallExpression.copy() as KtCallExpression

    val psiFactory = KtPsiFactory(project)

    val argument = if (shouldLambdaParameterBeNamed(newCallExpression.getValueArgumentsInParentheses(), oldCallExpression)) {
        psiFactory.createArgument(replacement, functionLiteralArgumentName)
    }
    else {
        psiFactory.createArgument(replacement)
    }

    val functionLiteralArgument = newCallExpression.lambdaArguments.firstOrNull()!!
    val valueArgumentList = newCallExpression.valueArgumentList ?: psiFactory.createCallArguments("()")

    valueArgumentList.addArgument(argument)

    (functionLiteralArgument.prevSibling as? PsiWhiteSpace)?.delete()
    if (newCallExpression.valueArgumentList != null) {
        functionLiteralArgument.delete()
    }
    else {
        functionLiteralArgument.replace(valueArgumentList)
    }
    return oldCallExpression.replace(newCallExpression) as KtCallExpression
}

private fun shouldLambdaParameterBeNamed(args: List<ValueArgument>, callExpr: KtCallExpression): Boolean {
    if (args.any { it.isNamed() }) return true
    val calee = (callExpr.calleeExpression?.mainReference?.resolve() as? KtFunction) ?: return true
    return if (calee.valueParameters.any { it.isVarArg }) true else calee.valueParameters.size - 1 > args.size
}


fun KtCallExpression.moveFunctionLiteralOutsideParentheses() {
    assert(lambdaArguments.isEmpty())
    val argumentList = valueArgumentList!!
    val argument = argumentList.arguments.last()
    val expression = argument.getArgumentExpression()!!
    assert(expression.unpackFunctionLiteral() != null)

    val dummyCall = KtPsiFactory(this).createExpressionByPattern("foo()$0:'{}'", expression) as KtCallExpression
    val functionLiteralArgument = dummyCall.lambdaArguments.single()
    this.add(functionLiteralArgument)
    /* we should not remove empty parenthesis when callee is a call too - it won't parse */
    if (argumentList.arguments.size == 1 && calleeExpression !is KtCallExpression) {
        argumentList.delete()
    }
    else {
        argumentList.removeArgument(argument)
    }
}

fun KtBlockExpression.appendElement(element: KtElement, addNewLine: Boolean = false): KtElement {
    val rBrace = rBrace
    val newLine = KtPsiFactory(this).createNewLine()
    val anchor = if (rBrace == null) {
        val lastChild = lastChild
        if (lastChild !is PsiWhiteSpace) addAfter(newLine, lastChild)!! else lastChild
    }
    else {
        rBrace.prevSibling!!
    }
    val addedElement = addAfter(element, anchor)!! as KtElement
    if (addNewLine) {
        addAfter(newLine, addedElement)
    }
    return addedElement
}

//TODO: git rid of this method
fun PsiElement.deleteElementAndCleanParent() {
    val parent = parent

    deleteElementWithDelimiters(this)
    deleteChildlessElement(parent, this::class.java)
}

// Delete element if it doesn't contain children of a given type
private fun <T : PsiElement> deleteChildlessElement(element: PsiElement, childClass: Class<T>) {
    if (PsiTreeUtil.getChildrenOfType<T>(element, childClass) == null) {
        element.delete()
    }
}

// Delete given element and all the elements separating it from the neighboring elements of the same class
private fun deleteElementWithDelimiters(element: PsiElement) {
    val paramBefore = PsiTreeUtil.getPrevSiblingOfType<PsiElement>(element, element.javaClass)

    val from: PsiElement
    val to: PsiElement
    if (paramBefore != null) {
        from = paramBefore.nextSibling
        to = element
    }
    else {
        val paramAfter = PsiTreeUtil.getNextSiblingOfType<PsiElement>(element, element.javaClass)

        from = element
        to = if (paramAfter != null) paramAfter.prevSibling else element
    }

    val parent = element.parent

    parent.deleteChildRange(from, to)
}

fun PsiElement.deleteSingle() {
    CodeEditUtil.removeChild(parent?.node ?: return, node ?: return)
}

fun KtClass.getOrCreateCompanionObject(): KtObjectDeclaration {
    companionObjects.firstOrNull()?.let { return it }
    return addDeclaration(KtPsiFactory(this).createCompanionObject())
}

fun KtDeclaration.toDescriptor(): DeclarationDescriptor? {
    val bindingContext = analyze()
    // TODO: temporary code
    if (this is KtPrimaryConstructor) {
        return (this.getContainingClassOrObject().resolveToDescriptor() as ClassDescriptor).unsubstitutedPrimaryConstructor
    }

    val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
    if (descriptor is ValueParameterDescriptor) {
        return bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, descriptor]
    }
    return descriptor
}

//TODO: code style option whether to insert redundant 'public' keyword or not
fun KtModifierListOwner.setVisibility(visibilityModifier: KtModifierKeywordToken) {
    if (this is KtDeclaration) {
        val defaultVisibilityKeyword = implicitVisibility()

        if (visibilityModifier == defaultVisibilityKeyword) {
            this.visibilityModifierType()?.let { removeModifier(it) }
            return
        }
    }

    addModifier(visibilityModifier)
}

fun KtDeclaration.implicitVisibility(): KtModifierKeywordToken? =
        when {
            this is KtConstructor<*> -> {
                val klass = getContainingClassOrObject()
                if (klass is KtClass && (klass.isEnum() || klass.isSealed())) KtTokens.PRIVATE_KEYWORD
                else KtTokens.DEFAULT_VISIBILITY_KEYWORD
            }
            hasModifier(KtTokens.OVERRIDE_KEYWORD) -> {
                (resolveToDescriptor(BodyResolveMode.PARTIAL) as? CallableMemberDescriptor)
                        ?.overriddenDescriptors
                        ?.let { OverridingUtil.findMaxVisibility(it) }
                        ?.toKeywordToken()
            }
            else -> {
                KtTokens.DEFAULT_VISIBILITY_KEYWORD
            }
        }

fun KtModifierListOwner.canBePrivate() = modifierList?.hasModifier(KtTokens.ABSTRACT_KEYWORD) != true

fun KtModifierListOwner.canBeProtected(): Boolean {
    val parent = this.parent
    return when (parent) {
        is KtClassBody -> parent.parent is KtClass
        is KtParameterList -> parent.parent is KtPrimaryConstructor
        else -> false
    }
}

fun KtClass.isInheritable(): Boolean {
    return when (getModalityFromDescriptor()) {
        KtTokens.ABSTRACT_KEYWORD, KtTokens.OPEN_KEYWORD, KtTokens.SEALED_KEYWORD -> true
        else -> false
    }
}

fun KtDeclaration.isOverridable(): Boolean {
    val parent = parent
    if (!(parent is KtClassBody || parent is KtParameterList)) return false

    val klass = if (parent.parent is KtPrimaryConstructor)
        parent.parent.parent as? KtClass
    else
        parent.parent as? KtClass

    if (klass == null || (!klass.isInheritable() && !klass.isEnum())) return false

    if (this.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
        // 'private' is incompatible with 'open'
        return false
    }

    return when (getModalityFromDescriptor()) {
        KtTokens.ABSTRACT_KEYWORD, KtTokens.OPEN_KEYWORD -> true
        else -> false
    }
}

fun KtDeclaration.getModalityFromDescriptor(): KtModifierKeywordToken? {
    val descriptor = this.resolveToDescriptor(BodyResolveMode.PARTIAL)
    if (descriptor is MemberDescriptor) {
        return mapModality(descriptor.modality)
    }

    return null
}

fun KtDeclaration.implicitModality(): KtModifierKeywordToken {
    var predictedModality = predictImplicitModality()
    val bindingContext = analyze(BodyResolveMode.PARTIAL)
    val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] ?: return predictedModality
    val containingDescriptor = descriptor.containingDeclaration ?: return predictedModality

    val extensions = DeclarationAttributeAltererExtension.getInstances(this.project)
    for (extension in extensions) {
        val newModality = extension.refineDeclarationModality(
                this,
                descriptor as? ClassDescriptor,
                containingDescriptor,
                mapModalityToken(predictedModality),
                bindingContext,
                isImplicitModality = true)

        if (newModality != null) {
            predictedModality = mapModality(newModality)
        }
    }

    return predictedModality
}

fun mapModality(accurateModality: Modality): KtModifierKeywordToken = when (accurateModality) {
    Modality.FINAL -> KtTokens.FINAL_KEYWORD
    Modality.SEALED -> KtTokens.SEALED_KEYWORD
    Modality.OPEN -> KtTokens.OPEN_KEYWORD
    Modality.ABSTRACT -> KtTokens.ABSTRACT_KEYWORD
}

private fun mapModalityToken(modalityToken: IElementType): Modality = when (modalityToken) {
    KtTokens.FINAL_KEYWORD -> Modality.FINAL
    KtTokens.SEALED_KEYWORD -> Modality.SEALED
    KtTokens.OPEN_KEYWORD -> Modality.OPEN
    KtTokens.ABSTRACT_KEYWORD -> Modality.ABSTRACT
    else -> error("Unexpected modality keyword $modalityToken")
}

private fun KtDeclaration.predictImplicitModality(): KtModifierKeywordToken {
    if (this is KtClassOrObject) {
        if (this is KtClass && this.isInterface()) return KtTokens.ABSTRACT_KEYWORD
        return KtTokens.FINAL_KEYWORD
    }
    val klass = containingClassOrObject ?: return KtTokens.FINAL_KEYWORD
    if (hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
        if (klass.hasModifier(KtTokens.ABSTRACT_KEYWORD) ||
            klass.hasModifier(KtTokens.OPEN_KEYWORD) ||
            klass.hasModifier(KtTokens.SEALED_KEYWORD)) {
            return KtTokens.OPEN_KEYWORD
        }
    }
    if (klass is KtClass && klass.isInterface() && !hasModifier(KtTokens.PRIVATE_KEYWORD)) {
        return if (hasBody()) KtTokens.OPEN_KEYWORD else KtTokens.ABSTRACT_KEYWORD
    }
    return KtTokens.FINAL_KEYWORD
}

fun KtSecondaryConstructor.getOrCreateBody(): KtBlockExpression {
    bodyExpression?.let { return it }

    val delegationCall = getDelegationCall()
    val anchor = if (delegationCall.isImplicit) valueParameterList else delegationCall
    val newBody = KtPsiFactory(this).createEmptyBody()
    return addAfter(newBody, anchor) as KtBlockExpression
}

fun KtParameter.dropDefaultValue() {
    val from = equalsToken ?: return
    val to = defaultValue ?: from
    deleteChildRange(from, to)
}

fun dropEnclosingParenthesesIfPossible(expression: KtExpression): KtExpression {
    val parent = expression.parent as? KtParenthesizedExpression ?: return expression
    if (!KtPsiUtil.areParenthesesUseless(parent)) return expression
    return parent.replaced(expression)
}

fun KtTypeParameterListOwner.addTypeParameter(typeParameter: KtTypeParameter): KtTypeParameter? {
    typeParameterList?.let { return it.addParameter(typeParameter) }

    val list = KtPsiFactory(this).createTypeParameterList("<X>")
    list.parameters[0].replace(typeParameter)
    val leftAnchor = when (this) {
                         is KtClass -> nameIdentifier ?: getClassOrInterfaceKeyword()
                         is KtNamedFunction -> funKeyword
                         is KtProperty -> valOrVarKeyword
                         else -> null
                     } ?: return null
    return (addAfter(list, leftAnchor) as KtTypeParameterList).parameters.first()
}

fun KtNamedFunction.getOrCreateValueParameterList(): KtParameterList {
    valueParameterList?.let { return it }
    val parameterList = KtPsiFactory(this).createParameterList("()")
    val anchor = nameIdentifier ?: funKeyword!!
    return addAfter(parameterList, anchor) as KtParameterList
}

fun KtCallableDeclaration.setType(type: KotlinType, shortenReferences: Boolean = true) {
    if (type.isError) return
    setType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type), shortenReferences)
}

fun KtCallableDeclaration.setType(typeString: String, shortenReferences: Boolean = true) {
    val typeReference = KtPsiFactory(project).createType(typeString)
    setTypeReference(typeReference)
    if (shortenReferences) {
        ShortenReferences.DEFAULT.process(getTypeReference()!!)
    }
}

fun KtCallableDeclaration.setReceiverType(type: KotlinType) {
    if (type.isError) return
    val typeReference = KtPsiFactory(project).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
    setReceiverTypeReference(typeReference)
    ShortenReferences.DEFAULT.process(receiverTypeReference!!)
}

fun KtParameter.setDefaultValue(newDefaultValue: KtExpression): PsiElement? {
    defaultValue?.let { return it.replaced(newDefaultValue) }

    val psiFactory = KtPsiFactory(this)
    val eq = equalsToken ?: add(psiFactory.createEQ())
    return addAfter(newDefaultValue, eq) as KtExpression
}
