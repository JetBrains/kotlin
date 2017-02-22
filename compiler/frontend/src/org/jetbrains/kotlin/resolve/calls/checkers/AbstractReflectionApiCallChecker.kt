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
import org.jetbrains.kotlin.builtins.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.serialization.deserialization.NotFoundClasses
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.util.OperatorNameConventions

private val ANY_MEMBER_NAMES = setOf("equals", "hashCode", "toString")

/**
 * Checks that there are no usages of reflection API which will fail at runtime.
 */
abstract class AbstractReflectionApiCallChecker(
        private val module: ModuleDescriptor,
        private val notFoundClasses: NotFoundClasses,
        storageManager: StorageManager
) : CallChecker {
    protected abstract val isWholeReflectionApiAvailable: Boolean
    protected abstract fun report(element: PsiElement, context: CallCheckerContext)

    private val kPropertyClasses by storageManager.createLazyValue {
        val reflectionTypes = ReflectionTypes(module, notFoundClasses)
        setOf(reflectionTypes.kProperty0, reflectionTypes.kProperty1, reflectionTypes.kProperty2)
    }

    final override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (isWholeReflectionApiAvailable) return

        // Do not report the diagnostic on built-in sources
        if (isReflectionSource(reportOn)) return

        val descriptor = resolvedCall.resultingDescriptor
        val containingClass = descriptor.containingDeclaration as? ClassDescriptor ?: return
        if (!ReflectionTypes.isReflectionClass(containingClass)) return

        if (!isAllowedReflectionApi(descriptor, containingClass)) {
            report(reportOn, context)
        }
    }

    protected open fun isAllowedReflectionApi(descriptor: CallableDescriptor, containingClass: ClassDescriptor): Boolean {
        val name = descriptor.name
        return name.asString() in ANY_MEMBER_NAMES ||
            name == OperatorNameConventions.INVOKE ||
            name.asString() == "name" ||
            (name.asString() == "get" || name.asString() == "set") && containingClass.isKPropertyClass()
    }

    private fun ClassDescriptor.isKPropertyClass() = kPropertyClasses.any { kProperty -> DescriptorUtils.isSubclass(this, kProperty) }

    private fun isReflectionSource(reportOn: PsiElement): Boolean {
        val file = reportOn.containingFile as? KtFile ?: return false
        val fqName = file.packageFqName.toUnsafe()
        return fqName == KOTLIN_REFLECT_FQ_NAME.toUnsafe() || fqName.asString().startsWith(KOTLIN_REFLECT_FQ_NAME.asString() + ".")
    }
}

