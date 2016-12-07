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

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors.MISSING_DEPENDENCY_CLASS
import org.jetbrains.kotlin.diagnostics.Errors.PRE_RELEASE_CLASS
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.checkers.isComputingDeferredType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.NotFoundClasses
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.newLinkedHashSetWithExpectedSize

object MissingDependencyClassChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        for (diagnostic in collectDiagnostics(reportOn, resolvedCall.resultingDescriptor)) {
            context.trace.report(diagnostic)
        }
    }

    private fun collectDiagnostics(reportOn: PsiElement, descriptor: CallableDescriptor): Set<Diagnostic> {
        val result: MutableSet<Diagnostic> = newLinkedHashSetWithExpectedSize(1)

        fun consider(classDescriptor: ClassDescriptor) {
            if (classDescriptor is NotFoundClasses.MockClassDescriptor) {
                result.add(MISSING_DEPENDENCY_CLASS.on(reportOn, classDescriptor.fqNameSafe))
                return
            }

            val source = classDescriptor.source
            if (source is DeserializedContainerSource && source.isPreReleaseInvisible) {
                // TODO: if at least one PRE_RELEASE_CLASS is reported, display a hint to disable the diagnostic
                result.add(PRE_RELEASE_CLASS.on(reportOn, classDescriptor.fqNameSafe))
                return
            }

            (classDescriptor.containingDeclaration as? ClassDescriptor)?.let(::consider)
        }

        fun consider(type: KotlinType) {
            if (!isComputingDeferredType(type)) {
                (type.constructor.declarationDescriptor as? ClassDescriptor)?.let(::consider)
            }
        }

        descriptor.returnType?.let(::consider)
        descriptor.extensionReceiverParameter?.value?.type?.let(::consider)
        descriptor.valueParameters.forEach { consider(it.type) }

        return result
    }
}
