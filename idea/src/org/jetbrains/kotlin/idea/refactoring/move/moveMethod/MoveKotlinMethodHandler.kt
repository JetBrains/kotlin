package org.jetbrains.kotlin.idea.refactoring.move.moveMethod

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveHandlerDelegate
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral

class MoveKotlinMethodHandler : MoveHandlerDelegate() {
    private fun showErrorHint(project: Project, dataContext: DataContext?, message: String) {
        val editor = if (dataContext == null) null else CommonDataKeys.EDITOR.getData(dataContext)
        CommonRefactoringUtil.showErrorHint(project, editor, message, KotlinBundle.message("text.move.method"), null)
    }

    private fun invokeMoveMethodRefactoring(
        project: Project, method: KtNamedFunction, targetContainer: KtClassOrObject?, dataContext: DataContext?
    ) {
        var message: String? = null
        if (method.containingClassOrObject == null) return
        if (!method.manager.isInProject(method)) {
            message = KotlinBundle.message("text.move.method.is.not.supported.for.non.project.methods")
        } else if (method.mentionsTypeParameters()) {
            message = KotlinBundle.message("text.move.method.is.not.supported.for.generic.classes")
        } else if (method.hasModifier(KtTokens.OVERRIDE_KEYWORD) || method.hasModifier(KtTokens.OPEN_KEYWORD)) {
            message = KotlinBundle.message("text.move.method.is.not.supported.when.method.is.a.part.of.inheritance.hierarchy")
        }
        message?.let {
            showErrorHint(project, dataContext, message)
            return
        }
        MoveKotlinMethodDialog(method, collectSuitableVariables(method), targetContainer).show()
    }

    private fun collectSuitableVariables(method: KtNamedFunction): Map<KtNamedDeclaration, KtClass> {
        val sourceClassOrObject = method.containingClassOrObject ?: return emptyMap()
        val allVariables = mutableListOf<KtNamedDeclaration>()

        allVariables.addAll(method.valueParameters)
        allVariables.addAll(sourceClassOrObject.declarations.filterIsInstance<KtProperty>())
        allVariables.addAll(sourceClassOrObject.primaryConstructorParameters.filter { parameter -> parameter.hasValOrVar() })

        val variableToClassMap = LinkedHashMap<KtNamedDeclaration, KtClass>()
        for (variable in allVariables) {
            (variable.resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType?.let { type ->
                if (type.arguments.isEmpty()) {
                    val ktClass = type.constructor.declarationDescriptor?.findPsi() as? KtClass
                    if (ktClass != null && method.manager.isInProject(ktClass)) {
                        variableToClassMap[variable] = ktClass
                    }
                }
            }
        }
        return variableToClassMap
    }

    override fun canMove(elements: Array<PsiElement?>, targetContainer: PsiElement?, reference: PsiReference?): Boolean {
        if (elements.size != 1) return false
        val method = elements[0] as? KtNamedFunction ?: return false
        val sourceContainer = method.containingClassOrObject
        return (targetContainer == null || super.canMove(elements, targetContainer, reference))
                && sourceContainer != null && !sourceContainer.isObjectLiteral()
    }

    override fun isValidTarget(psiElement: PsiElement?, sources: Array<out PsiElement>): Boolean {
        return psiElement is KtClassOrObject && !psiElement.hasModifier(KtTokens.ANNOTATION_KEYWORD)
    }

    override fun doMove(project: Project, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?) {
        if (elements.size != 1) return
        val method = elements[0] as? KtNamedFunction ?: return
        val sourceContainer = method.containingClassOrObject
        if (sourceContainer == null || sourceContainer.isObjectLiteral()) return
        invokeMoveMethodRefactoring(project, elements[0] as KtNamedFunction, targetContainer as? KtClassOrObject, null)
    }

    override fun tryToMove(
        element: PsiElement, project: Project, dataContext: DataContext?, reference: PsiReference?, editor: Editor?
    ): Boolean {
        if (element is KtNamedFunction) {
            element.containingClassOrObject?.let { sourceContainer ->
                if (!sourceContainer.isObjectLiteral()) {
                    invokeMoveMethodRefactoring(project, element, null, dataContext)
                    return true
                }
            }
        }
        return false
    }

    private fun KtNamedFunction.mentionsTypeParameters(): Boolean {
        var ktClassOrObject = containingClassOrObject
        val typeParameters = mutableListOf<KtTypeParameter>()
        while (ktClassOrObject != null) {
            typeParameters.addAll(ktClassOrObject.typeParameters)
            ktClassOrObject = if (ktClassOrObject.hasModifier(KtTokens.INNER_KEYWORD)) ktClassOrObject.containingClassOrObject else null
        }
        collectDescendantsOfType<KtUserType>().forEach { userType ->
            if (userType.referenceExpression?.mainReference?.resolve() in typeParameters) return true
        }
        return false
    }

    override fun getActionName(elements: Array<out PsiElement>): String = "${KotlinBundle.message("text.move.method")}.."
}
