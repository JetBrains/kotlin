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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.CollectionLiteralKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.SimpleKotlinCallArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.isParameterOfAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.intersectWrappedTypes
import org.jetbrains.kotlin.types.checker.prepareArgumentTypeRegardingCaptureTypes
import org.jetbrains.kotlin.types.typeUtil.isNullableNothing
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal fun unexpectedArgument(argument: KotlinCallArgument): Nothing =
    error("Unexpected argument type: $argument, ${argument.javaClass.canonicalName}.")

// if expression is not stable and has smart casts, then we create this type
internal val ReceiverValueWithSmartCastInfo.unstableType: UnwrappedType?
    get() {
        if (isStable || !hasTypesFromSmartCasts())
            return if (isStable) null else receiverValue.type.unwrap()

        val intersectionType = intersectWrappedTypes(allOriginalTypes)

        return prepareArgumentTypeRegardingCaptureTypes(intersectionType) ?: intersectionType
    }

// with all smart casts if stable
val ReceiverValueWithSmartCastInfo.stableType: UnwrappedType
    get() {
        if (!isStable || !hasTypesFromSmartCasts())
            return receiverValue.type.unwrap()

        /*
         * We have to intersect types first as after capturing, subtyping relation may change and some type won't be excluded from intersection type.
         *
         * Example:
         *      allOriginalTypes = [Inv<out CharSequence>, Inv<String>]
         *      intersect(Inv<out CharSequence>, Inv<String>) = Inv<String>
         *      capture(Inv<String>) = Inv<String>
         * But with capturing first:
         *      capture(Inv<out CharSequence>) = Inv<CapturedType(out CharSequence)>
         *      capture(Inv<String>) = Inv<String>
         *      intersect(Inv<CapturedType(out CharSequence)>, Inv<String>) = Inv<CapturedType(out CharSequence)> & Inv<String>
         *
         * Such redundant type with captured argument may further lead to contradiction in constraint system or less exact solution.
         */
        val intersectionType = intersectWrappedTypes(allOriginalTypes)

        // Intersection type of Nothing with any flexible types will be Nothing!.
        // This is a bit incorrect as cast to Nothing? or Nothing can result only in Nothing? or Nothing,
        // otherwise it'll be possible to pass null to some non-nullable type
        if (intersectionType.isNullableNothing() && !intersectionType.isMarkedNullable) {
            return intersectionType.makeNullable().unwrap()
        }

        return prepareArgumentTypeRegardingCaptureTypes(intersectionType) ?: intersectionType
    }

internal fun KotlinCallArgument.getExpectedType(parameter: ParameterDescriptor, languageVersionSettings: LanguageVersionSettings) =
    if (
        this.isSpread ||
        this.isArrayAssignedAsNamedArgumentInAnnotation(parameter, languageVersionSettings) ||
        this.isArrayAssignedAsNamedArgumentInFunction(parameter, languageVersionSettings)
    ) {
        parameter.type.unwrap()
    } else {
        parameter.safeAs<ValueParameterDescriptor>()?.varargElementType?.unwrap() ?: parameter.type.unwrap()
    }

val ValueParameterDescriptor.isVararg: Boolean get() = varargElementType != null
val ParameterDescriptor.isVararg: Boolean get() = this.safeAs<ValueParameterDescriptor>()?.isVararg ?: false

/**
 * @return `true` iff the parameter has a default value, i.e. declares it, inherits it by overriding a parameter which has a default value,
 * or is a parameter of an 'actual' declaration, such that the corresponding 'expect' parameter has a default value.
 */
fun ValueParameterDescriptor.hasDefaultValue(): Boolean {
    return DFS.ifAny(
        listOf(this),
        { current -> current.overriddenDescriptors.map(ValueParameterDescriptor::getOriginal) },
        { it.declaresDefaultValue() || it.isActualParameterWithCorrespondingExpectedDefault }
    )
}

private fun ValueParameterDescriptor.checkExpectedParameter(checker: (ValueParameterDescriptor) -> Boolean): Boolean {
    val function = containingDeclaration
    if (function is FunctionDescriptor && function.isActual) {
        with(ExpectedActualResolver) {
            val expected = function.findCompatibleExpectedForActual(function.module).firstOrNull()
            return expected is FunctionDescriptor && checker(expected.valueParameters[index])
        }
    }
    return false
}

/**
 * The following two properties describe two different situations.
 *
 * Consider hierarchy:
 *
 *     expect open class A { fun foo(p: Int = 1) }
 *     expect open class B : A { fun foo(p: Int) }
 *
 *     actual open class A { actual fun foo(p: Int) }
 *     actual open class B : A { actual fun foo(p: Int) }
 *
 * For parameter `p` of method `foo`:
 * `isActualParameterWithAnyExpectedDefault` returns `true` for both actual A and B
 * `isActualParameterWithCorrespondingExpectedDefault` returns `true` for actual A, but `false` for actual B because expect B declaration
 *     doesn't have a default value
 */
val ValueParameterDescriptor.isActualParameterWithAnyExpectedDefault: Boolean
    get() = checkExpectedParameter { it.hasDefaultValue() }

/**
 * @see isActualParameterWithAnyExpectedDefault
 */
val ValueParameterDescriptor.isActualParameterWithCorrespondingExpectedDefault: Boolean
    get() = checkExpectedParameter { it.declaresDefaultValue() }

private fun KotlinCallArgument.isArrayAssignedAsNamedArgumentInAnnotation(
    parameter: ParameterDescriptor,
    languageVersionSettings: LanguageVersionSettings
): Boolean {
    if (!languageVersionSettings.supportsFeature(LanguageFeature.AssigningArraysToVarargsInNamedFormInAnnotations)) return false

    val isAllowedAssigningSingleElementsToVarargsInNamedForm =
        !languageVersionSettings.supportsFeature(LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm)

    if (isAllowedAssigningSingleElementsToVarargsInNamedForm && !isArrayOrArrayLiteral()) return false

    return this.argumentName != null && parameter.isVararg && isParameterOfAnnotation(parameter)
}

private fun KotlinCallArgument.isArrayAssignedAsNamedArgumentInFunction(
    parameter: ParameterDescriptor,
    languageVersionSettings: LanguageVersionSettings
): Boolean {
    if (!languageVersionSettings.supportsFeature(LanguageFeature.AllowAssigningArrayElementsToVarargsInNamedFormForFunctions)) return false

    val isAllowedAssigningSingleElementsToVarargsInNamedForm =
        !languageVersionSettings.supportsFeature(LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm)

    if (isAllowedAssigningSingleElementsToVarargsInNamedForm && !isArrayOrArrayLiteral()) return false

    return this.argumentName != null && parameter.isVararg
}

fun KotlinCallArgument.isArrayOrArrayLiteral(): Boolean {
    if (this is CollectionLiteralKotlinCallArgument) return true
    if (this !is SimpleKotlinCallArgument) return false

    val type = this.receiver.receiverValue.type
    return KotlinBuiltIns.isArrayOrPrimitiveArray(type)
}
