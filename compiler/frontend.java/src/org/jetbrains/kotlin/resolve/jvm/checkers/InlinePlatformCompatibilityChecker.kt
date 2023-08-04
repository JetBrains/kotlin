/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassOrPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor

class InlinePlatformCompatibilityChecker(val jvmTarget: JvmTarget) : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val resultingDescriptor = resolvedCall.resultingDescriptor as? CallableMemberDescriptor ?: return
        // TODO (KT-60971): distinguish the case when one property accessor is inline and the other isn't.
        if (!InlineUtil.isInline(resultingDescriptor) &&
            (resultingDescriptor !is PropertyDescriptor || !InlineUtil.isInline(resultingDescriptor.getter))
        ) {
            return
        }

        val propertyOrFun = DescriptorUtils.getDirectMember(resultingDescriptor)

        val inliningBytecodeVersion = getBytecodeVersionIfDeserializedDescriptor(propertyOrFun) ?: return

        if (jvmTarget.majorVersion < inliningBytecodeVersion) {
            context.trace.report(
                ErrorsJvm.INLINE_FROM_HIGHER_PLATFORM.on(
                    reportOn,
                    JvmTarget.getDescription(inliningBytecodeVersion),
                    JvmTarget.getDescription(jvmTarget.majorVersion)
                )
            )
        }
    }

    companion object {
        private fun getBytecodeVersionIfDeserializedDescriptor(funOrProperty: DeclarationDescriptor): Int? {
            if (funOrProperty !is DeserializedCallableMemberDescriptor) return null

            val containingDeclaration =
                funOrProperty.getConcreteDeclarationForInline().containingDeclaration as ClassOrPackageFragmentDescriptor

            val source = containingDeclaration.source
            val binaryClass =
                when (source) {
                    is KotlinJvmBinarySourceElement -> source.binaryClass
                    is KotlinJvmBinaryPackageSourceElement -> source.getContainingBinaryClass(funOrProperty)
                    else -> null
                } as? FileBasedKotlinClass ?: return null

            return binaryClass.classVersion
        }

        private fun CallableMemberDescriptor.getConcreteDeclarationForInline(): CallableMemberDescriptor {
            if (!this.kind.isReal) {
                val superImplementation = overriddenDescriptors.firstOrNull {
                    val containingDeclaration = it.containingDeclaration
                    !DescriptorUtils.isInterface(containingDeclaration) && !DescriptorUtils.isAnnotationClass(containingDeclaration)
                }
                if (superImplementation != null) {
                    return superImplementation.getConcreteDeclarationForInline()
                }
            }
            return this
        }
    }
}
