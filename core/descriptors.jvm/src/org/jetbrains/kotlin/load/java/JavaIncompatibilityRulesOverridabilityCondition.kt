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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithDifferentJvmName.sameAsRenamedInJvmBuiltin
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.sameAsBuiltinMethodWithErasedValueParameters
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.kotlin.JvmType
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.load.kotlin.forceSingleValueParameterBoxing
import org.jetbrains.kotlin.load.kotlin.mapToJvmType
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition.Result
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.makeNullable

/**
 * This class contains Java-related overridability conditions that may force incompatibility
 */
class JavaIncompatibilityRulesOverridabilityCondition : ExternalOverridabilityCondition {
    override fun isOverridable(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        subClassDescriptor: ClassDescriptor?
    ): Result {
        if (isIncompatibleInAccordanceWithBuiltInOverridabilityRules(superDescriptor, subDescriptor, subClassDescriptor)) {
            return Result.INCOMPATIBLE
        }

        if (doesJavaOverrideHaveIncompatibleValueParameterKinds(superDescriptor, subDescriptor)) {
            return Result.INCOMPATIBLE
        }

        return Result.UNKNOWN
    }

    // This overridability condition checks two things:
    // 1. Method accidentally having the same signature as special builtin has does not supposed to be override for it in Java class
    // 2. In such Java class (with special signature clash) special builtin is loaded as hidden function with special signature, and
    // it should not override non-special method in further inheritance
    // See java.nio.Buffer
    private fun isIncompatibleInAccordanceWithBuiltInOverridabilityRules(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        subClassDescriptor: ClassDescriptor?
    ): Boolean {
        if (superDescriptor !is CallableMemberDescriptor || subDescriptor !is FunctionDescriptor ||
            KotlinBuiltIns.isBuiltIn(subDescriptor)
        ) {
            return false
        }

        if (!subDescriptor.name.sameAsBuiltinMethodWithErasedValueParameters && !subDescriptor.name.sameAsRenamedInJvmBuiltin) {
            return false
        }

        val overriddenBuiltin = superDescriptor.getOverriddenSpecialBuiltin()

        // Checking second condition: special hidden override is not supposed to be an override to non-special irrelevant Java declaration
        val isOneOfDescriptorsHidden =
            subDescriptor.isHiddenToOvercomeSignatureClash != (superDescriptor as? FunctionDescriptor)?.isHiddenToOvercomeSignatureClash
        if (isOneOfDescriptorsHidden &&
            (overriddenBuiltin == null || !subDescriptor.isHiddenToOvercomeSignatureClash)
        ) {
            return true
        }

        // If new containing class is not Java class or subDescriptor signature was artificially changed, use basic overridability rules
        if (subClassDescriptor !is JavaClassDescriptor || subDescriptor.initialSignatureDescriptor != null) {
            return false
        }

        // If current Java class has Kotlin super class with override of overriddenBuiltin, then common overridability rules can be applied
        // because of final special bridge generated in Kotlin super class
        if (overriddenBuiltin == null || subClassDescriptor.hasRealKotlinSuperClassWithOverrideOf(overriddenBuiltin)) return false

        // class A extends HashMap<Object, Object> {
        //    void get(Object x) {}
        // }
        //
        // The problem is that when checking overridability of `A.get` and `HashMap.get` we fall through to here, because
        // we do not recreate a magic copy of it, because it has the same signature.
        // But it obviously that if subDescriptor and superDescriptor has the same JVM descriptor, they're one-way overridable.
        // Note that it doesn't work if special builtIn was renamed, because we do not consider renamed built-ins
        // in `computeJvmDescriptor`.
        // TODO: things get more and more complicated here, consider moving signature mapping from backend and using it here instead of all of this magic
        if (overriddenBuiltin is FunctionDescriptor && superDescriptor is FunctionDescriptor &&
            BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(overriddenBuiltin) != null &&
            subDescriptor.computeJvmDescriptor(withReturnType = false) == superDescriptor.original.computeJvmDescriptor(withReturnType = false)
        ) {
            return false
        }

        // Here we know that something in Java with common signature is going to override some special builtin that is supposed to be
        // incompatible override
        return true
    }


    override fun getContract() = ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY

    companion object {
        /**
         * Checks if any pair of corresponding value parameters has different type kinds, e.g. one is primitive and another is not
         *
         * As it comes from it's name it only checks overrides in Java classes
         */
        fun doesJavaOverrideHaveIncompatibleValueParameterKinds(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor
        ): Boolean {
            if (subDescriptor !is JavaMethodDescriptor || superDescriptor !is FunctionDescriptor) return false
            assert(subDescriptor.valueParameters.size == superDescriptor.valueParameters.size) {
                "External overridability condition with CONFLICTS_ONLY should not be run with different value parameters size"
            }

            for ((subParameter, superParameter) in subDescriptor.original.valueParameters.zip(superDescriptor.original.valueParameters)) {
                val isSubPrimitive = mapValueParameterType(subDescriptor, subParameter) is JvmType.Primitive
                val isSuperPrimitive = mapValueParameterType(superDescriptor, superParameter) is JvmType.Primitive

                if (isSubPrimitive != isSuperPrimitive) {
                    return true
                }
            }

            return false
        }

        private fun mapValueParameterType(f: FunctionDescriptor, valueParameterDescriptor: ValueParameterDescriptor) =
            if (forceSingleValueParameterBoxing(f) || isPrimitiveCompareTo(f))
                valueParameterDescriptor.type.makeNullable().mapToJvmType()
            else
                valueParameterDescriptor.type.mapToJvmType()

        // It's useful here to suppose that 'Int.compareTo(Int)' requires boxing of it's value parameter
        // As it happens in java.lang.Integer analogue
        // It only affects additional built-ins loading (see 'testLoadBuiltIns' tests)
        private fun isPrimitiveCompareTo(f: FunctionDescriptor): Boolean {
            if (f.valueParameters.size != 1) return false
            val classDescriptor =
                f.containingDeclaration as? ClassDescriptor ?: return false
            val parameterClass =
                f.valueParameters.single().type.constructor.declarationDescriptor as? ClassDescriptor
                    ?: return false
            return KotlinBuiltIns.isPrimitiveClass(classDescriptor) && classDescriptor.fqNameSafe == parameterClass.fqNameSafe
        }
    }
}
