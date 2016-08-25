/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinRequestResultProcessor
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.search.unionSafe
import org.jetbrains.kotlin.idea.search.usagesSearch.DestructuringDeclarationUsageSearch.*
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.fuzzyExtensionReceiverType
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.dataClassUtils.getComponentIndex
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.isValidOperator
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.util.*

enum class DestructuringDeclarationUsageSearch {
    ALWAYS_SMART, ALWAYS_PLAIN, PLAIN_WHEN_NEEDED
}

var destructuringDeclarationUsageSearchMode = if (ApplicationManager.getApplication().isUnitTestMode) ALWAYS_SMART else PLAIN_WHEN_NEEDED

//TODO: compiled code
//TODO: check if it's too expensive

fun findDestructuringDeclarationUsages(
        componentFunction: PsiMethod,
        scope: SearchScope,
        consumer: Processor<PsiReference>,
        optimizer: SearchRequestCollector
) {
    if (componentFunction !is KtLightMethod) return //TODO
    val ktDeclarationTarget = componentFunction.kotlinOrigin as? KtDeclaration ?: return //TODO?
    findDestructuringDeclarationUsages(ktDeclarationTarget, scope, consumer, optimizer)
}

fun findDestructuringDeclarationUsages(
        ktDeclaration: KtDeclaration,
        scope: SearchScope,
        consumer: Processor<PsiReference>,
        optimizer: SearchRequestCollector
) {
    val usePlainSearch = when (destructuringDeclarationUsageSearchMode) {
        ALWAYS_SMART -> false
        ALWAYS_PLAIN -> true
        PLAIN_WHEN_NEEDED -> scope is LocalSearchScope // for local scope it's faster to use plain search
    }
    if (usePlainSearch) {
        doPlainSearch(ktDeclaration, scope, optimizer)
        return
    }

    val descriptor = ktDeclaration.resolveToDescriptor() as? CallableDescriptor ?: return

    if (descriptor is FunctionDescriptor && !descriptor.isValidOperator()) return

    val dataType = if (descriptor.isExtension) {
        descriptor.fuzzyExtensionReceiverType()!!
    }
    else {
        val classDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return
        classDescriptor.defaultType.toFuzzyType(classDescriptor.typeConstructor.parameters)
    }

    Processor(dataType,
              ktDeclaration,
              scope,
              consumer,
              plainSearchHandler = { searchScope -> doPlainSearch(ktDeclaration, searchScope, optimizer) }
    ).run()
}

private fun doPlainSearch(ktDeclaration: KtDeclaration, scope: SearchScope, optimizer: SearchRequestCollector) {
    val unwrappedElement = ktDeclaration.namedUnwrappedElement ?: return
    val resultProcessor = KotlinRequestResultProcessor(unwrappedElement,
                                                       filter = { ref -> ref is KtDestructuringDeclarationReference })
    optimizer.searchWord("(", scope.restrictToKotlinSources(), UsageSearchContext.IN_CODE, true, unwrappedElement, resultProcessor)
}

private class Processor(
        private val dataType: FuzzyType,
        private val target: KtDeclaration,
        private val searchScope: SearchScope,
        private val consumer: Processor<PsiReference>,
        private val plainSearchHandler: (SearchScope) -> Unit
) {
    private val project = target.project
    private val declarationsToSearch = ArrayList<PsiElement>()
    private val declarationsToSearchSet = HashSet<PsiElement>()
    private var scopeToUsePlainSearch: SearchScope = LocalSearchScope.EMPTY

    fun run() {
        val dataClassDescriptor = dataType.type.constructor.declarationDescriptor ?: return
        val dataClassDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, dataClassDescriptor)
        val psiClass = when (dataClassDeclaration) {
            is PsiClass -> dataClassDeclaration
            is KtClassOrObject -> dataClassDeclaration.toLightClass() ?: return
            else -> return
        }

        val parameters = ClassInheritorsSearch.SearchParameters(psiClass, GlobalSearchScope.allScope(project), true, true, false)
        val classesToSearch = listOf(psiClass) + ClassInheritorsSearch.search(parameters).findAll()

        for (classToSearch in classesToSearch) {
            ReferencesSearch.search(classToSearch).forEach(Processor processor@ { reference -> //TODO: see KT-13607
                if (processDataClassUsage(reference)) return@processor true

                if (destructuringDeclarationUsageSearchMode != ALWAYS_SMART) {
                    plainSearchHandler(searchScope)
                    return@processor false
                }

                val element = reference.element
                val document = PsiDocumentManager.getInstance(project).getDocument(element.containingFile)
                val lineAndCol = DiagnosticUtils.offsetToLineAndColumn(document, element.startOffset)
                error("Unsupported reference: '${element.text}' in ${element.containingFile.name} line ${lineAndCol.line} column ${lineAndCol.column}")
            })

            // we must use plain search inside our data class (and inheritors) because implicit 'this' can happen anywhere
            (classToSearch as? KtLightClass)?.kotlinOrigin?.let { usePlainSearch(it) }
        }

        // we use index instead of iterator because elements are added during iteration
        var index = 0
        while (index < declarationsToSearch.size) {
            val declaration = declarationsToSearch[index++]
            processDeclarationOfTypeWithDataClass(declaration)
        }

        plainSearchHandler(scopeToUsePlainSearch)
    }

    //TODO: check if it's operator (too expensive)
    private fun addDeclarationToProcess(declaration: PsiElement) {
        if (declarationsToSearchSet.add(declaration)) {
            declarationsToSearch.add(declaration)
        }
    }

    /**
     * Process usage of our data class or one of its inheritors
     */
    private fun processDataClassUsage(reference: PsiReference): Boolean {
        val element = reference.element
        return when (element.language) {
            KotlinLanguage.INSTANCE -> processKotlinDataClassUsage(element)

            JavaLanguage.INSTANCE -> processJavaDataClassUsage(element)

            else -> false // we don't know anything about usages in other languages - so we downgrade to slow algorithm in this case
        }
    }

    private fun processKotlinDataClassUsage(element: PsiElement): Boolean {
        //TODO: type aliases

        when (element) {
            is KtReferenceExpression -> {
                val parent = element.parent
                when (parent) {
                    is KtUserType -> {
                        val typeRef = parent.parents.lastOrNull { it is KtTypeReference }
                        val typeRefParent = typeRef?.parent
                        when (typeRefParent) {
                            is KtCallableDeclaration -> {
                                when (typeRef) {
                                    typeRefParent.typeReference -> {
                                        addDeclarationToProcess(typeRefParent)
                                        return true
                                    }

                                    typeRefParent.receiverTypeReference -> {
                                        // we must use plain search inside extensions because implicit 'this' can happen anywhere
                                        usePlainSearch(typeRefParent)
                                        return true
                                    }
                                }
                            }

                            is KtTypeProjection -> {
                                val callExpression = (typeRefParent.parent as? KtTypeArgumentList)?.parent as? KtCallExpression
                                if (callExpression != null) {
                                    processSuspiciousExpression(callExpression)
                                    return true
                                }
                            }

                            is KtConstructorCalleeExpression -> {
                                if (typeRefParent.parent is KtSuperTypeCallEntry) {
                                    // usage in super type list - just ignore, inheritors are processed above
                                    return true
                                }
                            }

                            is KtIsExpression -> {
                                val scopeOfPossibleSmartCast = typeRefParent.getParentOfType<KtDeclarationWithBody>(true)
                                scopeOfPossibleSmartCast?.let { usePlainSearch(it) }
                                return true
                            }

                            is KtBinaryExpressionWithTypeRHS -> {
                                processSuspiciousExpression(typeRefParent)
                                return true
                            }
                        }
                    }

                    is KtCallExpression -> {
                        if (element == parent.calleeExpression) {
                            processSuspiciousExpression(parent)
                            return true
                        }
                    }
                }

                if (element.getStrictParentOfType<KtImportDirective>() != null) return true // ignore usage in import
            }

            is KDocName -> return true // ignore usage in doc-comment
        }

        return false // unsupported type of reference
    }

    private fun processJavaDataClassUsage(element: PsiElement): Boolean {
        if (element !is PsiJavaCodeReferenceElement) return true // meaningless reference from Java

        ParentsLoop@
        for (parent in element.parents) {
            when (parent) {
                is PsiCodeBlock,
                is PsiExpression,
                is PsiImportStatement,
                is PsiParameter ->
                    break@ParentsLoop // ignore local usages, usages in imports and in parameter types

                is PsiMethod, is PsiField -> {
                    if (!(parent as PsiModifierListOwner).isPrivateOrLocal()) {
                        addDeclarationToProcess(parent)
                    }
                    break@ParentsLoop
                }
            }
        }

        return true
    }

    /**
     * Process declaration whose type is our data class (or data class used anywhere inside that type)
     */
    private fun processDeclarationOfTypeWithDataClass(declaration: PsiElement) {
        // we don't need to search usages of declarations in Java because Java doesn't have implicitly typed declarations so such usages cannot affect Kotlin code
        //TODO: what about Scala and other JVM-languages?
        val declarationUsageScope = GlobalSearchScope.projectScope(declaration.project).restrictToKotlinSources()
        ReferencesSearch.search(declaration, declarationUsageScope).forEach { reference ->
            if (reference is KtDestructuringDeclarationReference) { // declaration usage in form of destructuring declaration
                val entries = reference.element.entries
                val componentIndex = when (declaration) {
                    is KtParameter -> declaration.dataClassComponentFunction()?.name?.asString()?.let { getComponentIndex(it) }
                    is KtFunction -> declaration.name?.let { getComponentIndex(it) }
                    //TODO: java component functions (see KT-13605)
                    else -> null
                }
                if (componentIndex != null && componentIndex <= entries.size) {
                    processDeclarationOfTypeWithDataClass(entries[componentIndex - 1])
                }
            }
            else {
                (reference.element as? KtReferenceExpression)?.let { processSuspiciousExpression(it) }
            }
        }
    }

    /**
     * Process expression which may have type of our data class (or data class used anywhere inside that type)
     */
    private fun processSuspiciousExpression(expression: KtExpression) {
        ParentsLoop@
        for (container in expression.parentsWithSelf) {
            if (container !is KtExpression) continue
            val parent = container.parent

            when (parent) {
                is KtDestructuringDeclaration -> {
                    processSuspiciousDeclaration(parent)
                    break@ParentsLoop
                }

                is KtWithExpressionInitializer -> {
                    if (container != parent.initializer) continue@ParentsLoop

                    processSuspiciousDeclaration(parent)

                    break@ParentsLoop
                }

                is KtContainerNode -> {
                    if (parent.node.elementType == KtNodeTypes.LOOP_RANGE) {
                        val forExpression = parent.parent as KtForExpression
                        (forExpression.destructuringParameter ?: forExpression.loopParameter as KtDeclaration?)?.let {
                            processSuspiciousDeclaration(it)
                        }
                    }
                }
            }
        }
    }

    /**
     * Process declaration which may have implicit type of our data class (or data class used anywhere inside that type)
     */
    private fun processSuspiciousDeclaration(declaration: KtDeclaration) {
        if (declaration is KtDestructuringDeclaration) {
            if (searchScope.contains(declaration)) {
                val declarationReference = declaration.references.firstIsInstance<KtDestructuringDeclarationReference>()
                if (declarationReference.isReferenceTo(target)) {
                    consumer.process(declarationReference)
                }
            }
        }
        else {
            if (!isImplicitlyTyped(declaration)) return

            val descriptor = declaration.resolveToDescriptorIfAny() as? CallableDescriptor ?: return
            val type = descriptor.returnType
            if (type != null && type.containsTypeOrDerivedInside(dataType)) {
                addDeclarationToProcess(declaration)
            }
        }
    }

    private fun usePlainSearch(scopeElement: KtElement) {
        scopeToUsePlainSearch = LocalSearchScope(scopeElement)
                .intersectWith(searchScope)
                .unionSafe(scopeToUsePlainSearch)
    }

    private fun PsiModifierListOwner.isPrivateOrLocal(): Boolean {
        return hasModifierProperty(PsiModifier.PRIVATE) || parents.any { it is PsiCodeBlock }
    }

    private fun KotlinType.containsTypeOrDerivedInside(type: FuzzyType): Boolean {
        return type.checkIsSuperTypeOf(this) != null || arguments.any { it.type.containsTypeOrDerivedInside(type) }
    }

    private fun isImplicitlyTyped(declaration: KtDeclaration): Boolean {
        return when (declaration) {
            is KtFunction -> !declaration.hasDeclaredReturnType()
            is KtVariableDeclaration -> declaration.typeReference == null
            is KtParameter -> declaration.typeReference == null
            else -> false
        }
    }
}
