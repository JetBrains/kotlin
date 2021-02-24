/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.SmartList
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.codeInsight.shorten.addDelayedImportRequest
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.plugin.references.SimpleNameReferenceExtension
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.expressions.OperatorConventions

class KtSimpleNameReferenceDescriptorsImpl(
    expression: KtSimpleNameExpression
) : KtSimpleNameReference(expression), KtDescriptorsBasedReference {
    override fun doCanBeReferenceTo(candidateTarget: PsiElement): Boolean =
        canBeReferenceTo(candidateTarget)

    override fun isReferenceToWithoutExtensionChecking(candidateTarget: PsiElement): Boolean =
        matchesTarget(candidateTarget)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        return SmartList<DeclarationDescriptor>().apply {
            // Replace Java property with its accessor(s)
            for (descriptor in expression.getReferenceTargets(context)) {
                val sizeBefore = size

                if (descriptor !is JavaPropertyDescriptor) {
                    add(descriptor)
                    continue
                }

                val readWriteAccess = expression.readWriteAccess(true)
                descriptor.getter?.let {
                    if (readWriteAccess.isRead) add(it)
                }
                descriptor.setter?.let {
                    if (readWriteAccess.isWrite) add(it)
                }

                if (size == sizeBefore) {
                    add(descriptor)
                }
            }
        }
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (!canBeReferenceTo(element)) return false

        for (extension in Extensions.getArea(element.project).getExtensionPoint(SimpleNameReferenceExtension.EP_NAME).extensions) {
            if (extension.isReferenceTo(this, element)) return true
        }

        return super<KtDescriptorsBasedReference>.isReferenceTo(element)
    }

    override fun getRangeInElement(): TextRange {
        val element = element.getReferencedNameElement()
        val startOffset = getElement().startOffset
        return element.textRange.shiftRight(-startOffset)
    }

    override fun canRename(): Boolean {
        if (expression.getParentOfTypeAndBranch<KtWhenConditionInRange>(strict = true) { operationReference } != null) return false

        val elementType = expression.getReferencedNameElementType()
        if (elementType == KtTokens.PLUSPLUS || elementType == KtTokens.MINUSMINUS) return false

        return true
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        if (!canRename()) throw IncorrectOperationException()

        if (newElementName.unquote() == "") {
            return when (val qualifiedElement = expression.getQualifiedElement()) {
                is KtQualifiedExpression -> {
                    expression.replace(qualifiedElement.receiverExpression)
                    qualifiedElement.replaced(qualifiedElement.selectorExpression!!)
                }
                is KtUserType -> expression.replaced(
                    KtPsiFactory(expression).createSimpleName(
                        SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.asString()
                    )
                )
                else -> expression
            }
        }

        // Do not rename if the reference corresponds to synthesized component function
        val expressionText = expression.text
        if (expressionText != null && Name.isValidIdentifier(expressionText)) {
            if (DataClassDescriptorResolver.isComponentLike(Name.identifier(expressionText)) && resolve() is KtParameter) {
                return expression
            }
        }

        val psiFactory = KtPsiFactory(expression)
        val element = Extensions.getArea(expression.project).getExtensionPoint(SimpleNameReferenceExtension.EP_NAME).extensions
            .asSequence()
            .map { it.handleElementRename(this, psiFactory, newElementName) }
            .firstOrNull { it != null } ?: psiFactory.createNameIdentifier(newElementName.quoteIfNeeded())

        val nameElement = expression.getReferencedNameElement()

        val elementType = nameElement.node.elementType
        if (elementType is KtToken && OperatorConventions.getNameForOperationSymbol(elementType) != null) {
            val opExpression = expression.parent as? KtOperationExpression
            if (opExpression != null) {
                val (newExpression, newNameElement) = OperatorToFunctionIntention.convert(opExpression)
                newNameElement.replace(element)
                return newExpression
            }
        }

        if (element.node.elementType == KtTokens.IDENTIFIER) {
            nameElement.astReplace(element)
        } else {
            nameElement.replace(element)
        }
        return expression
    }


    // By default reference binding is delayed
    override fun bindToElement(element: PsiElement): PsiElement =
        bindToElement(element, ShorteningMode.DELAYED_SHORTENING)

    override fun bindToElement(element: PsiElement, shorteningMode: ShorteningMode): PsiElement =
        element.getKotlinFqName()?.let { fqName -> bindToFqName(fqName, shorteningMode, element) } ?: expression

    override fun bindToFqName(
        fqName: FqName,
        shorteningMode: ShorteningMode,
        targetElement: PsiElement?
    ): PsiElement {
        val expression = expression
        if (fqName.isRoot) return expression

        // not supported for infix calls and operators
        if (expression !is KtNameReferenceExpression) return expression
        if (expression.parent is KtThisExpression || expression.parent is KtSuperExpression) return expression // TODO: it's a bad design of PSI tree, we should change it

        val newExpression = expression.changeQualifiedName(
            fqName.quoteIfNeeded().let {
                if (shorteningMode == ShorteningMode.NO_SHORTENING)
                    it
                else
                    it.withRootPrefixIfNeeded(expression)
            },
            targetElement
        )
        val newQualifiedElement = newExpression.getQualifiedElementOrCallableRef()

        if (shorteningMode == ShorteningMode.NO_SHORTENING) return newExpression

        val needToShorten = PsiTreeUtil.getParentOfType(expression, KtImportDirective::class.java, KtPackageDirective::class.java) == null
        if (!needToShorten) {
            return newExpression
        }

        return when (shorteningMode) {
            ShorteningMode.FORCED_SHORTENING -> ShortenReferences.DEFAULT.process(newQualifiedElement)
            else -> {
                newQualifiedElement.addToShorteningWaitSet()
                newExpression
            }
        }
    }

    /**
     * Replace [[KtNameReferenceExpression]] (and its enclosing qualifier) with qualified element given by FqName
     * Result is either the same as original element, or [[KtQualifiedExpression]], or [[KtUserType]]
     * Note that FqName may not be empty
     */
    private fun KtNameReferenceExpression.changeQualifiedName(
        fqName: FqName,
        targetElement: PsiElement? = null
    ): KtNameReferenceExpression {
        assert(!fqName.isRoot) { "Can't set empty FqName for element $this" }

        val shortName = fqName.shortName().asString()
        val psiFactory = KtPsiFactory(this)
        val parent = parent

        if (parent is KtUserType && !fqName.isOneSegmentFQN()) {
            val qualifier = parent.qualifier
            val qualifierReference = qualifier?.referenceExpression as? KtNameReferenceExpression
            if (qualifierReference != null && qualifier.typeArguments.isNotEmpty()) {
                qualifierReference.changeQualifiedName(fqName.parent(), targetElement)
                return this
            }
        }

        val targetUnwrapped = targetElement?.unwrapped

        if (targetUnwrapped != null && targetUnwrapped.isTopLevelKtOrJavaMember() && fqName.isOneSegmentFQN()) {
            addDelayedImportRequest(targetUnwrapped, containingKtFile)
        }

        var parentDelimiter = "."
        val fqNameBase = when {
            parent is KtCallElement -> {
                val callCopy = parent.copied()
                callCopy.calleeExpression!!.replace(psiFactory.createSimpleName(shortName)).parent!!.text
            }
            parent is KtCallableReferenceExpression && parent.callableReference == this -> {
                parentDelimiter = ""
                val callableRefCopy = parent.copied()
                callableRefCopy.receiverExpression?.delete()
                val newCallableRef = callableRefCopy
                    .callableReference
                    .replace(psiFactory.createSimpleName(shortName))
                    .parent as KtCallableReferenceExpression
                if (targetUnwrapped != null && targetUnwrapped.isTopLevelKtOrJavaMember()) {
                    addDelayedImportRequest(targetUnwrapped, parent.containingKtFile)
                    return parent.replaced(newCallableRef).callableReference as KtNameReferenceExpression
                }
                newCallableRef.text
            }
            else -> shortName
        }

        val text = if (!fqName.isOneSegmentFQN()) "${fqName.parent().asString()}$parentDelimiter$fqNameBase" else fqNameBase

        val elementToReplace = getQualifiedElementOrCallableRef()

        val newElement = when (elementToReplace) {
            is KtUserType -> {
                val typeText = "$text${elementToReplace.typeArgumentList?.text ?: ""}"
                elementToReplace.replace(psiFactory.createType(typeText).typeElement!!)
            }
            else -> KtPsiUtil.safeDeparenthesize(elementToReplace.replaced(psiFactory.createExpression(text)))
        } as KtElement

        val selector = (newElement as? KtCallableReferenceExpression)?.callableReference
            ?: newElement.getQualifiedElementSelector()
            ?: error("No selector for $newElement")
        return selector as KtNameReferenceExpression
    }

    override fun getCanonicalText(): String = expression.text

    override val resolvesByNames: Collection<Name>
        get() {
            val element = element

            if (element is KtOperationReferenceExpression) {
                val tokenType = element.operationSignTokenType
                if (tokenType != null) {
                    val name = OperatorConventions.getNameForOperationSymbol(
                        tokenType, element.parent is KtUnaryExpression, element.parent is KtBinaryExpression
                    ) ?: return emptyList()
                    val counterpart = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS[tokenType]
                    return if (counterpart != null) {
                        val counterpartName = OperatorConventions.getNameForOperationSymbol(counterpart, false, true)!!
                        listOf(name, counterpartName)
                    } else {
                        listOf(name)
                    }
                }
            }

            return listOf(element.getReferencedNameAsName())
        }

    override fun getImportAlias(): KtImportAlias? {
        fun DeclarationDescriptor.unwrap() = if (this is ImportedFromObjectCallableDescriptor<*>) callableFromObject else this

        val element = element
        val name = element.getReferencedName()
        val file = element.containingKtFile
        val importDirective = file.findImportByAlias(name) ?: return null
        val fqName = importDirective.importedFqName ?: return null
        val importedDescriptors = file.resolveImportReference(fqName).map { it.unwrap() }
        if (getTargetDescriptors(element.analyze(BodyResolveMode.PARTIAL)).any {
                it.unwrap().getImportableDescriptor() in importedDescriptors
            }) {
            return importDirective.alias
        }
        return null
    }
}
