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

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithDifferentJvmName.sameAsRenamedInJvmBuiltin
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.sameAsBuiltinMethodWithErasedValueParameters
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition.Result
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.isDocumentedAnnotation

class JavaIncompatibilityRulesOverridabilityCondition : ExternalOverridabilityCondition {
    override fun isOverridable(superDescriptor: CallableDescriptor, subDescriptor: CallableDescriptor, subClassDescriptor: ClassDescriptor?): Result {
        if (superDescriptor !is CallableMemberDescriptor || subDescriptor !is FunctionDescriptor || subDescriptor.isFromBuiltins()) {
            return Result.UNKNOWN
        }

        if (!subDescriptor.name.sameAsBuiltinMethodWithErasedValueParameters && !subDescriptor.name.sameAsRenamedInJvmBuiltin) {
            return Result.UNKNOWN
        }

        // This overridability condition checks two things:
        // 1. Method accidentally having the same signature as special builtin has does not supposed to be override for it in Java class
        // 2. In such Java class (with special signature clash) special builtin is loaded as hidden function with special signature, and
        // it should not override non-special method in further inheritance
        // See java.nio.Buffer

        val overriddenBuiltin = superDescriptor.getOverriddenSpecialBuiltin()

        // Checking second condition: special hidden override is not supposed to be an override to non-special irrelevant Java declaration
        val isOneOfDescriptorsHidden =
                subDescriptor.isHiddenToOvercomeSignatureClash != (superDescriptor as? FunctionDescriptor)?.isHiddenToOvercomeSignatureClash
        if ((overriddenBuiltin == null || !subDescriptor.isHiddenToOvercomeSignatureClash) && isOneOfDescriptorsHidden) {
            return Result.INCOMPATIBLE
        }

        // If new containing class is not Java class or subDescriptor signature was artificially changed, use basic overridability rules
        if (subClassDescriptor !is JavaClassDescriptor || subDescriptor.initialSignatureDescriptor != null) {
            return Result.UNKNOWN
        }

        // If current Java class has Kotlin super class with override of overriddenBuiltin, then common overridability rules can be applied
        // because of final special bridge generated in Kotlin super class
        if (subClassDescriptor.hasRealKotlinSuperClassWithOverrideOf(overriddenBuiltin ?: return Result.UNKNOWN)) return Result.UNKNOWN

        // Here we know that something in Java with common signature is going to override some special builtin that is supposed to be
        // incompatible override
        return Result.INCOMPATIBLE
    }

    override fun getContract() = ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY
}
