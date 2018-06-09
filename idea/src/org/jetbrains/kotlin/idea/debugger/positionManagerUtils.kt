/*
 * Copyright 2010-2015 KtBrains s.r.o.
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
import com.intellij.psi.util.PsiTreeUtil
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.ReferenceType
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.fileClasses.getFileClassInternalName
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.debugger.breakpoints.getLambdasAtLineIfAny
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.resolve.inline.InlineUtil

fun DebugProcess.getAllClassesAtLine(position: SourcePosition): List<ReferenceType> {
    val result = hashSetOf<ReferenceType>()

    result.addAll(getAllClassesAtElement(position.elementAt, position.line))

    getLambdasAtLineIfAny(position).forEach {
        result.addAll(getAllClassesAtElement(it, position.line))
    }

    return result.toList()
}

private fun DebugProcess.getAllClassesAtElement(elementAt: PsiElement, lineAt: Int): List<ReferenceType> {

    var depthToLocalOrAnonymousClass = 0

    fun calc(element: KtElement?): String? {
        when (element) {
            null -> return null
            is KtClassOrObject -> {
                if (element.isLocal) {
                    depthToLocalOrAnonymousClass++
                    return calc(element.getElementToCalculateClassName())
                }

                return element.getNameForNonAnonymousClass()
            }
            is KtFunctionLiteral -> {
                if (!isInlinedLambda(element)) {
                    depthToLocalOrAnonymousClass++
                }
                return calc(element.getElementToCalculateClassName())
            }
            is KtClassInitializer -> {
                val parent = element.getElementToCalculateClassName()

                if (parent is KtObjectDeclaration && parent.isCompanion()) {
                    // Companion-object initializer
                    return calc(parent.getElementToCalculateClassName())
                }

                return calc(parent)
            }
            is KtPropertyAccessor -> {
                return calc(element.getClassOfFile())
            }
            is KtProperty -> {
                if (element.isTopLevel || element.isLocal) {
                    return calc(element.getElementToCalculateClassName())
                }

                val containingClass = element.getClassOfFile()
                if (containingClass is KtObjectDeclaration && containingClass.isCompanion()) {
                    // Properties from companion object are moved into class
                    val descriptor = element.resolveToDescriptor() as PropertyDescriptor
                    if (AsmUtil.isPropertyWithBackingFieldCopyInOuterClass(descriptor)) {
                        return calc(containingClass.getElementToCalculateClassName())
                    }
                }

                return calc(containingClass)
            }
            is KtSecondaryConstructor -> {
                return calc(element.getElementToCalculateClassName())
            }
            is KtNamedFunction -> {
                if (element.name == null) {
                    val descriptor = element.readAction { InlineUtil.getInlineArgumentDescriptor(it, element.analyze()) }
                    if (descriptor == null || descriptor.isCrossinline) {
                        depthToLocalOrAnonymousClass++
                    }
                }
                else if (element.isLocal) {
                    depthToLocalOrAnonymousClass++
                }
                val parent = element.getElementToCalculateClassName()
                if (parent is KtClassInitializer) {
                    // TODO BUG? anonymous functions from companion object constructor should be inner class of companion object, not class
                    return calc(parent.getElementToCalculateClassName())
                }

                return calc(parent)
            }
            is KtFile -> {
                return NoResolveFileClassesProvider.getFileClassInternalName(element)
            }
            else -> throw IllegalStateException("Unsupported container ${element.javaClass}")
        }
    }

    val elementToCalcClassName = elementAt.getElementToCalculateClassName(true)
    val className = calc(elementToCalcClassName) ?: return emptyList()

    if (depthToLocalOrAnonymousClass == 0) {
        return virtualMachineProxy.classesByName(className)
    }

    // the name is a parent class for a local or anonymous class
    val outers = virtualMachineProxy.classesByName(className)
    return outers.map { findNested(it, 0, depthToLocalOrAnonymousClass, elementAt, lineAt) }.filterNotNull()
}

private fun KtClassOrObject.getNameForNonAnonymousClass(addTraitImplSuffix: Boolean = true): String? {
    if (isLocal) return null
    if (this.isObjectLiteral()) return null

    val name = name ?: return null

    val parentClass = PsiTreeUtil.getParentOfType(this, KtClassOrObject::class.java, true)
    if (parentClass != null) {
        val shouldAddTraitImplSuffix = !(parentClass is KtClass && this is KtObjectDeclaration && this.isCompanion())
        val parentName = parentClass.getNameForNonAnonymousClass(shouldAddTraitImplSuffix)
        if (parentName == null) {
            return null
        }
        return parentName + "$" + name
    }

    val className = if (addTraitImplSuffix && this is KtClass && this.isInterface()) name + JvmAbi.DEFAULT_IMPLS_SUFFIX else name

    val packageFqName = this.containingKtFile.packageFqName
    return if (packageFqName.isRoot) className else packageFqName.asString() + "." + className
}

private val TYPES_TO_CALCULATE_CLASSNAME: Array<Class<out KtElement>> =
        arrayOf(KtFile::class.java,
                KtClass::class.java,
                KtObjectDeclaration::class.java,
                KtEnumEntry::class.java,
                KtFunctionLiteral::class.java,
                KtNamedFunction::class.java,
                KtPropertyAccessor::class.java,
                KtProperty::class.java,
                KtClassInitializer::class.java,
                KtSecondaryConstructor::class.java)

private fun PsiElement?.getElementToCalculateClassName(withItSelf: Boolean = false): KtElement? {
    if (withItSelf && this?.let { it::class.java } as Class<*> in TYPES_TO_CALCULATE_CLASSNAME) return this as KtElement

    return readAction { PsiTreeUtil.getParentOfType(this, *TYPES_TO_CALCULATE_CLASSNAME) }
}

private fun PsiElement.getClassOfFile(): KtElement? {
    return PsiTreeUtil.getParentOfType(this, KtFile::class.java, KtClassOrObject::class.java)
}

private fun DebugProcess.findNested(
        fromClass: ReferenceType,
        currentDepth: Int,
        requiredDepth: Int,
        elementAt: PsiElement,
        lineAt: Int
): ReferenceType? {
    val vmProxy = virtualMachineProxy
    if (fromClass.isPrepared) {
        try {
            if (currentDepth < requiredDepth) {
                val nestedTypes = vmProxy.nestedTypes(fromClass)
                for (nested in nestedTypes) {
                    val found = findNested(nested, currentDepth + 1, requiredDepth, elementAt, lineAt)
                    if (found != null) {
                        return found
                    }
                }
                return null
            }

            for (location in fromClass.allLineLocations()) {
                val locationLine = location.lineNumber() - 1
                if (locationLine <= 0) {
                    // such locations are not correspond to real lines in code
                    continue
                }
                val method = location.method()
                if (method == null || DebuggerUtils.isSynthetic(method) || method.isBridge) {
                    // skip synthetic methods
                    continue
                }

                if (lineAt == locationLine) {
//                    TODO: compare elementAt, can be significant for lambdas
//                    val candidatePosition = KotlinPositionManager(this).getSourcePosition(location)
//                    if (candidatePosition?.elementAt == elementAt) {
                        return fromClass
//                    }
                }
            }
        }
        catch (ignored: AbsentInformationException) {
        }

    }
    return null
}

private fun isInlinedLambda(functionLiteral: KtFunctionLiteral): Boolean {
    val functionLiteralExpression = functionLiteral.parent

    var parent = functionLiteralExpression.parent

    while (parent is KtParenthesizedExpression || parent is KtBinaryExpressionWithTypeRHS || parent is KtLabeledExpression) {
        parent = parent.parent
    }

    while (parent is ValueArgument || parent is KtValueArgumentList) {
        parent = parent.parent
    }

    if (parent !is KtElement) return false

    return InlineUtil.isInlinedArgument(functionLiteral, functionLiteral.analyze(), true)
}