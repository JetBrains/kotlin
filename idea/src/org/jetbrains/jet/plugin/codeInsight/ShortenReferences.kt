package org.jetbrains.jet.plugin.codeInsight

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.Collections;
import java.util.HashSet;
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.HashMap
import java.util.ArrayList

public object ShortenReferences {
    public fun process(element: JetElement) {
        process(Collections.singleton(element))
    }

    public fun process(elements: Iterable<JetElement>) {
        val first = elements.firstOrNull()
        if (first == null) return
        val file = first.getContainingFile() as JetFile

        // first resolve all qualified references - optimization
        val resolveAllVisitor = ResolveAllReferencesVisitor(file)
        processElements(elements, resolveAllVisitor)

        val shortenTypesVisitor = ShortenTypesVisitor(file, resolveAllVisitor.result)
        processElements(elements, shortenTypesVisitor)
        shortenTypesVisitor.finish()

        processElements(elements, ShortenQualifiedExpressionsVisitor(file, resolveAllVisitor.result))
    }

    private fun processElements(elements: Iterable<JetElement>, visitor: JetVisitorVoid) {
        for (element in elements) {
            element.accept(visitor)
        }
    }

    private class ResolveAllReferencesVisitor(file: JetFile) : JetVisitorVoid() {
        private val resolveSession = AnalyzerFacadeWithCache.getLazyResolveSessionForFile(file)
        private val resolveMap = HashMap<JetReferenceExpression, BindingContext>()

        public val result: Map<JetReferenceExpression, BindingContext> = resolveMap

        override fun visitJetElement(element : JetElement) {
            element.acceptChildren(this)
        }

        override fun visitUserType(userType: JetUserType) {
            userType.acceptChildren(this)

            if (userType.getQualifier() != null) {
                val referenceExpression = userType.getReferenceExpression()
                if (referenceExpression != null) {
                    resolveMap[referenceExpression] = resolveSession.resolveToElement(referenceExpression)
                }
            }
        }

        override fun visitDotQualifiedExpression(expression: JetDotQualifiedExpression) {
            expression.acceptChildren(this)

            val referenceExpression = referenceExpression(expression.getSelectorExpression())
            if (referenceExpression != null) {
                resolveMap[referenceExpression] = resolveSession.resolveToElement(referenceExpression)
            }
        }
    }

    private class ShortenTypesVisitor(val file: JetFile, val resolveMap: Map<JetReferenceExpression, BindingContext>) : JetVisitorVoid() {
        private val resolveSession : ResolveSessionForBodies
            get() = AnalyzerFacadeWithCache.getLazyResolveSessionForFile(file)

        private val typesToShorten = ArrayList<JetUserType>()

        public fun finish() {
            for (userType in typesToShorten) {
                shortenType(userType)
            }
        }

        private fun bindingContext(expression: JetReferenceExpression): BindingContext = resolveMap[expression]!!

        override fun visitJetElement(element : JetElement) {
            element.acceptChildren(this)
        }

        override fun visitUserType(userType: JetUserType) {
            userType.getTypeArgumentList()?.accept(this)

            if (canShortenType(userType)) {
                typesToShorten.add(userType)
            }
            else{
                userType.getQualifier()?.accept(this)
            }
        }

        private fun canShortenType(userType: JetUserType): Boolean {
            if (userType.getQualifier() == null) return false
            val referenceExpression = userType.getReferenceExpression()
            if (referenceExpression == null) return false

            val target = bindingContext(referenceExpression).get(BindingContext.REFERENCE_TARGET, referenceExpression)
            if (target == null) return false
            // references to nested classes should be shortened when visiting qualifier
            if (target.getContainingDeclaration() is ClassDescriptor) return false

            val typeReference = PsiTreeUtil.getParentOfType(userType, javaClass<JetTypeReference>())!!
            val scope = resolveSession.resolveToElement(typeReference).get(BindingContext.TYPE_RESOLUTION_SCOPE, typeReference)!!
            val name = target.getName()
            val targetByName = scope.getClassifier(name) ?: scope.getPackage(name)
            if (targetByName == null) {
                addImportIfNeeded(target, file)
                return true
            }
            else if (target.asString() == targetByName.asString()) {
                return true
            }
            else {
                // leave FQ name
                return false
            }
        }

        private fun shortenType(userType: JetUserType) {
            val referenceExpression = userType.getReferenceExpression()
            if (referenceExpression == null) return
            val typeArgumentList = userType.getTypeArgumentList()
            val text = referenceExpression.getText() + (if (typeArgumentList != null) typeArgumentList.getText() else "")
            val newUserType = JetPsiFactory.createType(userType.getProject(), text).getTypeElement()!!
            userType.replace(newUserType)
        }
    }

    private class ShortenQualifiedExpressionsVisitor(val file: JetFile, val resolveMap: Map<JetReferenceExpression, BindingContext>) : JetVisitorVoid() {
        private val resolveSession : ResolveSessionForBodies
            get() = AnalyzerFacadeWithCache.getLazyResolveSessionForFile(file)

        private fun bindingContext(expression: JetReferenceExpression): BindingContext
                = resolveMap[expression] ?: resolveSession.resolveToElement(expression) // binding context can be absent in the map if some references have been shortened already

        override fun visitJetElement(element : JetElement) {
            acceptChildren(element)
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
                        return shortenIfPossible(qualifiedExpression, targetClass, bindingContext(calleeExpression))
                    }
                }
            }
            else if (selectorExpression is JetReferenceExpression) {
                val bindingContext = bindingContext(selectorExpression)
                val target = bindingContext.get(BindingContext.REFERENCE_TARGET, selectorExpression)
                if (target is ClassDescriptor || target is PackageViewDescriptor) { //TODO: should we ever add imports to real packages?
                    return shortenIfPossible(qualifiedExpression, target, bindingContext)
                }
            }
            return qualifiedExpression
        }

        private fun instantiatedClass(calleeExpression: JetReferenceExpression): ClassDescriptor? {
            val bindingContext = bindingContext(calleeExpression)
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

        private fun shortenIfPossible(qualifiedExpression: JetDotQualifiedExpression, targetClassOrPackage: DeclarationDescriptor, bindingContext: BindingContext): PsiElement {
            // references to nested classes should be shortened when visiting qualifier
            if (targetClassOrPackage.getContainingDeclaration() is ClassDescriptor) return qualifiedExpression

            val referenceExpression = referenceExpression(qualifiedExpression.getSelectorExpression())!!
            val resolveBefore = resolveState(referenceExpression, bindingContext)

            val copy = qualifiedExpression.copy()

            val selectorExpression = qualifiedExpression.getSelectorExpression()!!
            val newExpression = qualifiedExpression.replace(selectorExpression) as JetExpression
            val newReferenceExpression = referenceExpression(newExpression)!!

            val newBindingContext = resolveSession.resolveToElement(newReferenceExpression)
            val resolveAfter = resolveState(newReferenceExpression, newBindingContext)
            if (resolveAfter != null) {
                if (resolveBefore == resolveAfter) return newExpression
                return newExpression.replace(copy) // revert shortening
            }

            addImportIfNeeded(targetClassOrPackage, file)
            return newExpression
        }

        private fun resolveState(referenceExpression: JetReferenceExpression, bindingContext: BindingContext): Any? {
            val target = bindingContext.get(BindingContext.REFERENCE_TARGET, referenceExpression)
            if (target != null) return target.asString()

            val targets = bindingContext.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression)
            if (targets != null) return HashSet(targets.map{it!!.asString()})

            return null
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

    private fun referenceExpression(selectorExpression: JetExpression?): JetReferenceExpression?
            = if (selectorExpression is JetCallExpression)
                  selectorExpression.getCalleeExpression() as? JetReferenceExpression
              else
                  selectorExpression as? JetReferenceExpression

    private fun DeclarationDescriptor.asString() = DescriptorRenderer.TEXT.render(this)

    //TODO: do we need this "IfNeeded" check?
    private fun addImportIfNeeded(descriptor: DeclarationDescriptor, file: JetFile) {
        ImportInsertHelper.addImportDirectiveIfNeeded(DescriptorUtils.getFqNameSafe(descriptor), file)
    }
}

//TODO: how about such function in stdlib?
fun <T: Any> Iterable<T>.firstOrNull() : T? {
    val iterator = this.iterator()
    return if (iterator.hasNext()) iterator.next() else null
}

