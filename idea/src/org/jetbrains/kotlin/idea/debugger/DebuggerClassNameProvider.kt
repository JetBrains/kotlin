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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.DebuggerUtils
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.ReferenceType
import org.jetbrains.kotlin.codegen.binding.CodegenBinding.asmTypeForAnonymousClass
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.fileClasses.getFileClassInternalName
import org.jetbrains.kotlin.idea.debugger.breakpoints.getLambdasAtLineIfAny
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.Companion.getOrComputeClassNames
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.ComputedClassNames
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.ComputedClassNames.Companion.Cached
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.ComputedClassNames.Companion.EMPTY
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.ComputedClassNames.Companion.NonCached
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.org.objectweb.asm.Type
import java.util.*

class DebuggerClassNameProvider(
        private val debugProcess: DebugProcess,
        scopes: List<GlobalSearchScope>,
        val findInlineUseSites: Boolean = true,
        val alwaysReturnLambdaParentClass: Boolean = true
) {
    companion object {
        internal val CLASS_ELEMENT_TYPES = arrayOf<Class<out PsiElement>>(
                KtFile::class.java,
                KtClassOrObject::class.java,
                KtProperty::class.java,
                KtNamedFunction::class.java,
                KtFunctionLiteral::class.java,
                KtAnonymousInitializer::class.java)

        internal fun getRelevantElement(element: PsiElement): PsiElement? {
            for (elementType in CLASS_ELEMENT_TYPES) {
                if (elementType.isInstance(element)) {
                    return element
                }
            }

            // Do not copy the array (*elementTypes) if the element is one we look for
            return runReadAction { PsiTreeUtil.getNonStrictParentOfType(element, *CLASS_ELEMENT_TYPES) }
        }
    }

    private val inlineUsagesSearcher = InlineCallableUsagesSearcher(debugProcess, scopes)

    /**
     * Returns classes in which the given line number *is* present.
     */
    fun getClassesForPosition(position: SourcePosition): List<ReferenceType> = with (debugProcess) {
        val lineNumber = position.line

        return doGetClassesForPosition(position)
                .flatMap { className -> virtualMachineProxy.classesByName(className) }
                .flatMap { referenceType -> findTargetClasses(referenceType, lineNumber) }
    }

    /**
     * Returns classes names in JDI format (my.app.App$Nested) in which the given line number *may be* present.
     */
    fun getOuterClassNamesForPosition(position: SourcePosition): List<String> {
        return doGetClassesForPosition(position).toList()
    }

    private fun doGetClassesForPosition(position: SourcePosition): Set<String> {
        val relevantElement = runReadAction { getRelevantElement(position.elementAt) }

        val result = getOrComputeClassNames(relevantElement) { element ->
            getOuterClassNamesForElement(element)
        }.toMutableSet()

        for (lambda in position.readAction(::getLambdasAtLineIfAny)) {
            result += getOrComputeClassNames(lambda) { element ->
                getOuterClassNamesForElement(element)
            }
        }

        return result
    }

    @PublishedApi
    @Suppress("NON_TAIL_RECURSIVE_CALL")
    internal tailrec fun getOuterClassNamesForElement(element: PsiElement?): ComputedClassNames {
        if (element == null) return EMPTY

        return when (element) {
            is KtFile -> {
                val fileClassName = runReadAction { NoResolveFileClassesProvider.getFileClassInternalName(element) }.toJdiName()
                ComputedClassNames.Cached(fileClassName)
            }
            is KtClassOrObject -> {
                val enclosingElementForLocal = runReadAction { KtPsiUtil.getEnclosingElementForLocalDeclaration(element) }
                when {
                    enclosingElementForLocal != null ->
                        // A local class
                        getOuterClassNamesForElement(enclosingElementForLocal)
                    runReadAction { element.isObjectLiteral() } ->
                        getOuterClassNamesForElement(element.relevantParentInReadAction)
                    else ->
                        // Guaranteed to be non-local class or object
                        element.readAction {
                            if (it is KtClass && runReadAction { it.isInterface() }) {
                                val name = getNameForNonLocalClass(it)

                                if (name != null)
                                    Cached(listOf(name, name + JvmAbi.DEFAULT_IMPLS_SUFFIX))
                                else
                                    ComputedClassNames.EMPTY
                            }
                            else {
                                getNameForNonLocalClass(it)?.let { ComputedClassNames.Cached(it) } ?: ComputedClassNames.EMPTY
                            }
                        }
                }
            }
            is KtProperty -> {
                val nonInlineClasses = if (runReadAction { element.isTopLevel }) {
                    // Top level property
                    getOuterClassNamesForElement(element.relevantParentInReadAction)
                }
                else {
                    val enclosingElementForLocal = runReadAction { KtPsiUtil.getEnclosingElementForLocalDeclaration(element) }
                    if (enclosingElementForLocal != null) {
                        // Local class
                        getOuterClassNamesForElement(enclosingElementForLocal)
                    }
                    else {
                        val containingClassOrFile = runReadAction {
                            PsiTreeUtil.getParentOfType(element, KtFile::class.java, KtClassOrObject::class.java)
                        }

                        if (containingClassOrFile is KtObjectDeclaration && containingClassOrFile.isCompanionInReadAction) {
                            // Properties from the companion object can be placed in the companion object's containing class
                            (getOuterClassNamesForElement(containingClassOrFile.relevantParentInReadAction) +
                                    getOuterClassNamesForElement(containingClassOrFile)).distinct()
                        }
                        else if (containingClassOrFile != null) {
                            getOuterClassNamesForElement(containingClassOrFile)
                        }
                        else {
                            getOuterClassNamesForElement(element.relevantParentInReadAction)
                        }
                    }
                }

                if (findInlineUseSites && (
                        element.isInlineInReadAction ||
                        runReadAction { element.accessors.any { it.hasModifier(KtTokens.INLINE_KEYWORD) } })
                ) {
                    nonInlineClasses + inlineUsagesSearcher.findInlinedCalls(element) { this.getOuterClassNamesForElement(it) }
                }
                else {
                    return NonCached(nonInlineClasses.classNames)
                }
            }
            is KtNamedFunction -> {
                val typeMapper = KotlinDebuggerCaches.getOrCreateTypeMapper(element)

                val classNamesOfContainingDeclaration = getOuterClassNamesForElement(element.relevantParentInReadAction)

                val nonInlineClasses: ComputedClassNames = if (runReadAction { element.name == null || element.isLocal }) {
                    classNamesOfContainingDeclaration + ComputedClassNames.Cached(
                            asmTypeForAnonymousClass(typeMapper.bindingContext, element).internalName.toJdiName())
                }
                else {
                    classNamesOfContainingDeclaration
                }

                if (!findInlineUseSites || !element.isInlineInReadAction) {
                    return NonCached(nonInlineClasses.classNames)
                }

                val inlineCallSiteClasses = inlineUsagesSearcher.findInlinedCalls(element) { this.getOuterClassNamesForElement(it) }

                nonInlineClasses + inlineCallSiteClasses
            }
            is KtAnonymousInitializer -> {
                val initializerOwner = runReadAction { element.containingDeclaration }

                if (initializerOwner is KtObjectDeclaration && initializerOwner.isCompanionInReadAction) {
                    return getOuterClassNamesForElement(runReadAction { initializerOwner.containingClassOrObject })
                }

                getOuterClassNamesForElement(initializerOwner)
            }
            is KtFunctionLiteral -> {
                val typeMapper = KotlinDebuggerCaches.getOrCreateTypeMapper(element)

                val nonInlinedLambdaClassName = runReadAction {
                    asmTypeForAnonymousClass(typeMapper.bindingContext, element).internalName.toJdiName()
                }

                if (!alwaysReturnLambdaParentClass && !InlineUtil.isInlinedArgument(element, typeMapper.bindingContext, true)) {
                    return ComputedClassNames.Cached(nonInlinedLambdaClassName)
                }

                ComputedClassNames.Cached(nonInlinedLambdaClassName) + getOuterClassNamesForElement(element.relevantParentInReadAction)
            }
            else -> getOuterClassNamesForElement(element.relevantParentInReadAction)
        }
    }

    private fun getNameForNonLocalClass(nonLocalClassOrObject: KtClassOrObject): String? {
        val typeMapper = KotlinDebuggerCaches.getOrCreateTypeMapper(nonLocalClassOrObject)
        val descriptor = typeMapper.bindingContext[BindingContext.CLASS, nonLocalClassOrObject] ?: return null

        val type = typeMapper.mapClass(descriptor)
        if (type.sort != Type.OBJECT) {
            return null
        }

        return type.className
    }

    private val KtDeclaration.isInlineInReadAction: Boolean
        get() = runReadAction { hasModifier(KtTokens.INLINE_KEYWORD) }

    private val KtObjectDeclaration.isCompanionInReadAction: Boolean
        get() = runReadAction { isCompanion() }

    private val PsiElement.relevantParentInReadAction
        get() = runReadAction { getRelevantElement(this.parent) }
}

private fun String.toJdiName() = replace('/', '.')

private fun DebugProcess.findTargetClasses(outerClass: ReferenceType, lineAt: Int): List<ReferenceType> {
    val vmProxy = virtualMachineProxy
    if (!outerClass.isPrepared) return emptyList()

    val targetClasses = ArrayList<ReferenceType>(1)

    try {
        for (location in outerClass.allLineLocations()) {
            val locationLine = location.lineNumber() - 1
            if (locationLine < 0) {
                // such locations are not correspond to real lines in code
                continue
            }

            if (lineAt == locationLine) {
                val method = location.method()
                if (method == null || DebuggerUtils.isSynthetic(method) || method.isBridge) {
                    // skip synthetic methods
                    continue
                }

                targetClasses += outerClass
                break
            }
        }

        // The same line number may appear in different classes so we have to scan nested classes as well.
        // For example, in the next example line 3 appears in both Foo and Foo$Companion.

        /* class Foo {
            companion object {
                val a = Foo() /* line 3 */
            }
        } */

        val nestedTypes = vmProxy.nestedTypes(outerClass)
        for (nested in nestedTypes) {
            targetClasses += findTargetClasses(nested, lineAt)
        }
    }
    catch (_: AbsentInformationException) {}

    return targetClasses
}
