package org.jetbrains.jet.plugin.codeInsight

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.project.CancelableResolveSession;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.Collections;
import java.util.HashSet;
import com.intellij.psi.util.PsiTreeUtil

public object ShortenReferences {
    public fun process(element: JetElement) {
        process(Collections.singleton(element))
    }

    public fun process(elements: Iterable<JetElement>) {
        val first = elements.firstOrNull()
        if (first == null) return

        val visitor = Visitor(first.getContainingFile() as JetFile)
        for (element in elements) {
            element.accept(visitor)
        }
    }

    private class Visitor(val file: JetFile) : JetVisitorVoid() {
        private var modificationCount = file.getManager()!!.getModificationTracker().getModificationCount()
        private val resolveSession : CancelableResolveSession = AnalyzerFacadeWithCache.getLazyResolveSessionForFile(file)
          get(){
              val currentModificationCount = file.getManager()!!.getModificationTracker().getModificationCount()
              if (currentModificationCount != modificationCount) {
                  $resolveSession = AnalyzerFacadeWithCache.getLazyResolveSessionForFile(file)
                  modificationCount = currentModificationCount
              }
              return $resolveSession
          }

        override fun visitJetElement(element : JetElement) {
            acceptChildren(element)
        }

        override fun visitUserType(userType: JetUserType) {
            val resultElement = processType(userType)
            acceptChildren(resultElement)
        }

        private fun processType(userType: JetUserType): PsiElement {
            if (userType.getQualifier() == null) return userType

            val bindingContext = resolveSession.resolveToElement(userType)
            val target = bindingContext.get(BindingContext.REFERENCE_TARGET, userType.getReferenceExpression())
            if (target == null) return userType
            // references to nested classes should be shortened when visiting qualifier
            if (target.getContainingDeclaration() is ClassDescriptor) return userType

            val typeReference = PsiTreeUtil.getParentOfType(userType, javaClass<JetTypeReference>())!!
            val scope = bindingContext.get(BindingContext.TYPE_RESOLUTION_SCOPE, typeReference)!!
            val name = target.getName()
            val targetByName = scope.getClassifier(name) ?: scope.getPackage(name)
            if (target == targetByName) {
                return shortenType(userType)
            }
            else if (targetByName == null) {
                addImportIfNeeded(target)
                return shortenType(userType)
            }
            else {
                // leave FQ name
                return userType
            }
        }

        private fun shortenType(userType: JetUserType): JetUserType {
            val referenceExpression = userType.getReferenceExpression()
            if (referenceExpression == null) return userType
            val typeArgumentList = userType.getTypeArgumentList()
            val text = referenceExpression.getText() + (if (typeArgumentList != null) typeArgumentList.getText() else "")
            val newUserType = JetPsiFactory.createType(userType.getProject(), text).getTypeElement() as JetUserType
            return userType.replace(newUserType) as JetUserType
        }

        override fun visitDotQualifiedExpression(expression: JetDotQualifiedExpression) {
            val resultElement = processDotQualifiedExpression(expression)
            acceptChildren(resultElement)
        }

        private fun processDotQualifiedExpression(qualifiedExpression: JetDotQualifiedExpression): PsiElement {
            val selectorExpression = qualifiedExpression.getSelectorExpression()
            if (selectorExpression is JetCallExpression) {
                val calleeExpression = selectorExpression.getCalleeExpression()
                if (calleeExpression is JetReferenceExpression) {
                    val targetClass = instantiatedClass(calleeExpression)
                    if (targetClass != null) {
                        return shortenIfPossible(qualifiedExpression, targetClass)
                    }
                }
            }
            else if (selectorExpression is JetReferenceExpression) {
                val bindingContext = resolveSession.resolveToElement(selectorExpression)
                val target = bindingContext.get(BindingContext.REFERENCE_TARGET, selectorExpression)
                if (target is ClassDescriptor || target is PackageViewDescriptor) { //TODO: should we ever add imports to real packages?
                    return shortenIfPossible(qualifiedExpression, target)
                }
            }
            return qualifiedExpression
        }

        private fun instantiatedClass(calleeExpression: JetReferenceExpression): ClassDescriptor? {
            val bindingContext = resolveSession.resolveToElement(calleeExpression)
            val target = bindingContext.get(BindingContext.REFERENCE_TARGET, calleeExpression)
            if (target != null) {
                if (target is ConstructorDescriptor) {
                    return target.getContainingDeclaration()
                }
            }
            else {
                val targets = bindingContext.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, calleeExpression)
                if (targets != null && !targets.isEmpty()) {
                    var targetClass: ClassDescriptor? = null
                    for (descriptor in targets) {
                        if (descriptor is ConstructorDescriptor) {
                            val classDescriptor = descriptor.getContainingDeclaration().getOriginal() as ClassDescriptor
                            if (targetClass == null || targetClass == classDescriptor) {
                                targetClass = classDescriptor
                                continue
                            }
                        }
                        return null
                    }
                    return targetClass
                }
            }
            return null
        }

        private fun shortenIfPossible(qualifiedExpression: JetDotQualifiedExpression, targetClassOrPackage: DeclarationDescriptor): PsiElement {
            // references to nested classes should be shortened when visiting qualifier
            if (targetClassOrPackage.getContainingDeclaration() is ClassDescriptor) return qualifiedExpression

            var bindingContext = resolveSession.resolveToElement(qualifiedExpression)

            val referenceExpression = referenceExpression(qualifiedExpression.getSelectorExpression()!!)
            val resolveBefore = resolveState(referenceExpression, bindingContext)

            val copy = qualifiedExpression.copy()

            val selectorExpression = qualifiedExpression.getSelectorExpression()!!
            val newExpression = qualifiedExpression.replace(selectorExpression) as JetExpression
            val newReferenceExpression = referenceExpression(newExpression)

            bindingContext = resolveSession.resolveToElement(newReferenceExpression)
            val resolveAfter = resolveState(newReferenceExpression, bindingContext)
            if (resolveAfter != null) {
                if (resolveBefore == resolveAfter) return newExpression
                return newExpression.replace(copy) // revert shortening
            }

            addImportIfNeeded(targetClassOrPackage)
            return newExpression
        }

        private fun resolveState(referenceExpression: JetReferenceExpression, bindingContext: BindingContext): Any? {
            val target = bindingContext.get(BindingContext.REFERENCE_TARGET, referenceExpression)
            if (target != null) return DescriptorRenderer.TEXT.render(target.getOriginal())

            val targets = bindingContext.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression)
            if (targets != null) return HashSet(targets.map{DescriptorRenderer.TEXT.render(it!!)})

            return null
        }

        //TODO: do we need this "IfNeeded" check?
        private fun addImportIfNeeded(descriptor : DeclarationDescriptor) {
            ImportInsertHelper.addImportDirectiveIfNeeded(DescriptorUtils.getFqNameSafe(descriptor), file)
        }

        // we do not use standard PsiElement.acceptChildren because it won't work correctly if the element is replaced by the visitor
        private fun acceptChildren(element: PsiElement) {
            var child = element.getFirstChild()
            while(child != null) {
                val nextChild = child!!.getNextSibling()
                child!!.accept(this)
                child = nextChild
            }
        }
    }

    private fun referenceExpression(selectorExpression: JetExpression) = if (selectorExpression is JetCallExpression)
            selectorExpression.getCalleeExpression() as JetReferenceExpression
        else
            selectorExpression as JetReferenceExpression
}

//TODO: how about such function in stdlib?
fun <T: Any> Iterable<T>.firstOrNull() : T? {
    val iterator = this.iterator()
    return if (iterator.hasNext()) iterator.next() else null
}

