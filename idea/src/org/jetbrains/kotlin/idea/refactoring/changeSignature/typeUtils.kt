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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection
import java.util.LinkedHashMap
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.JetFunctionDefinitionUsage
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.types.Variance

private fun getTypeSubstitution(baseType: JetType, derivedType: JetType): LinkedHashMap<TypeConstructor, TypeProjection>? {
    val substitutedType = TypeCheckingProcedure.findCorrespondingSupertype(derivedType, baseType) ?: return null

    val substitution = LinkedHashMap<TypeConstructor, TypeProjection>(substitutedType.getArguments().size())
    for ((param, arg) in baseType.getConstructor().getParameters() zip substitutedType.getArguments()) {
        substitution[param.getTypeConstructor()] = arg
    }

    return substitution
}

private fun getFunctionSubstitution(
        baseFunction: FunctionDescriptor,
        derivedFunction: FunctionDescriptor
): MutableMap<TypeConstructor, TypeProjection>? {
    val baseClass = baseFunction.getContainingDeclaration() as? ClassDescriptor ?: return null
    val derivedClass = derivedFunction.getContainingDeclaration() as? ClassDescriptor ?: return null
    val substitution = getTypeSubstitution(baseClass.getDefaultType(), derivedClass.getDefaultType()) ?: return null

    for ((baseParam, derivedParam) in baseFunction.getTypeParameters() zip derivedFunction.getTypeParameters()) {
        substitution[baseParam.getTypeConstructor()] = TypeProjectionImpl(derivedParam.getDefaultType())
    }

    return substitution
}

fun getTypeSubstitutor(baseType: JetType, derivedType: JetType): TypeSubstitutor? {
    return getTypeSubstitution(baseType, derivedType)?.let { TypeSubstitutor.create(it) }
}

fun getFunctionSubstitutor(
        baseFunction: JetFunctionDefinitionUsage<*>,
        derivedFunction: JetFunctionDefinitionUsage<*>
): TypeSubstitutor? {
    val currentBaseFunction = baseFunction.getCurrentFunctionDescriptor()
    val currentDerivedFunction = derivedFunction.getCurrentFunctionDescriptor()
    if (currentBaseFunction == null || currentDerivedFunction == null) return null

    return getFunctionSubstitution(currentBaseFunction, currentDerivedFunction)?.let { TypeSubstitutor.create(it) }
}

fun JetType.renderTypeWithSubstitution(substitutor: TypeSubstitutor?, defaultText: String, inArgumentPosition: Boolean): String {
    val newType = substitutor?.substitute(this, Variance.INVARIANT) ?: return defaultText
    val renderer = if (inArgumentPosition) IdeDescriptorRenderers.SOURCE_CODE_FOR_TYPE_ARGUMENTS else IdeDescriptorRenderers.SOURCE_CODE
    return renderer.renderType(newType)
}
