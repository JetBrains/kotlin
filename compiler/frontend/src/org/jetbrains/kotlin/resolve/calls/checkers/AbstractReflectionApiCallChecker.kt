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

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

private val ALLOWED_MEMBER_NAMES = setOf(
    "equals", "hashCode", "toString", "invoke", "name"
)

private val ALLOWED_CLASSES = setOf(
    FqName("kotlin.reflect.KType"),
    FqName("kotlin.reflect.KTypeParameter"),
    FqName("kotlin.reflect.KTypeProjection"),
    FqName("kotlin.reflect.KTypeProjection.Companion"),
    FqName("kotlin.reflect.KVariance")
)

/**
 * Checks that there are no usages of reflection API which will fail at runtime.
 */
abstract class AbstractReflectionApiCallChecker(
    private val reflectionTypes: ReflectionTypes,
    storageManager: StorageManager
) : CallChecker {
    protected abstract val isWholeReflectionApiAvailable: Boolean
    protected abstract fun report(element: PsiElement, context: CallCheckerContext)

    private val kPropertyClasses by storageManager.createLazyValue {
        setOf(reflectionTypes.kProperty0, reflectionTypes.kProperty1, reflectionTypes.kProperty2)
    }

    private val kClass by storageManager.createLazyValue { reflectionTypes.kClass }

    protected open fun isAllowedKClassMember(name: Name, context: CallCheckerContext): Boolean = when (name.asString()) {
        "simpleName", "isInstance" -> true
        "qualifiedName" -> context.languageVersionSettings.getFlag(allowFullyQualifiedNameInKClass)
        else -> false
    }

    final override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (isWholeReflectionApiAvailable) return

        // Do not report the diagnostic on built-in sources
        if (isReflectionSource(reportOn)) return

        val descriptor = resolvedCall.resultingDescriptor
        val containingClass = descriptor.containingDeclaration as? ClassDescriptor ?: return
        if (!ReflectionTypes.isReflectionClass(containingClass)) return

        if (!isAllowedReflectionApi(descriptor, containingClass, context)) {
            report(reportOn, context)
        }
    }

    protected open fun isAllowedReflectionApi(
        descriptor: CallableDescriptor,
        containingClass: ClassDescriptor,
        context: CallCheckerContext
    ): Boolean {
        val name = descriptor.name
        return name.asString() in ALLOWED_MEMBER_NAMES ||
                DescriptorUtils.isSubclass(containingClass, kClass) && isAllowedKClassMember(name, context) ||
                (name.asString() == "get" || name.asString() == "set") && containingClass.isKPropertyClass() ||
                containingClass.fqNameSafe in ALLOWED_CLASSES
    }

    private fun ClassDescriptor.isKPropertyClass() = kPropertyClasses.any { kProperty -> DescriptorUtils.isSubclass(this, kProperty) }

    private fun isReflectionSource(reportOn: PsiElement): Boolean {
        val file = reportOn.containingFile as? KtFile ?: return false
        return file.packageFqName.startsWith(KOTLIN_REFLECT_FQ_NAME)
    }
}
