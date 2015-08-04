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

package org.jetbrains.kotlin.synthetic

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.load.java.typeEnhacement.enhanceSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.LookupLocation
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.DescriptorSubstitutor
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeSubstitution
import org.jetbrains.kotlin.types.Variance
import java.util.ArrayList
import java.util.LinkedHashSet

interface SamAdapterExtensionFunctionDescriptor : FunctionDescriptor {
    val originalFunction: FunctionDescriptor
}

class SamAdapterFunctionsScope(storageManager: StorageManager) : JetScope by JetScope.Empty {
    private val extensionForFunction = storageManager.createMemoizedFunctionWithNullableValues<FunctionDescriptor, FunctionDescriptor> { function ->
        extensionForFunctionNotCached(function)
    }

    private fun extensionForFunctionNotCached(function: FunctionDescriptor): FunctionDescriptor? {
        if (!function.visibility.isVisibleOutside()) return null
        if (!function.hasJavaOriginInHierarchy()) return null //TODO: should we go into base at all?
        if (!SingleAbstractMethodUtils.isSamAdapterNecessary(function)) return null
        if (function.returnType == null) return null
        //TODO: it's a temporary hack while original returns a function with platform types
        val enhancedFunction = function.enhanceSignature()
        return MyFunctionDescriptor(enhancedFunction)
    }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>, name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        var result: SmartList<FunctionDescriptor>? = null
        for (type in receiverTypes) {
            for (function in type.memberScope.getFunctions(name, location)) {
                val extension = extensionForFunction(function.original)
                if (extension != null) {
                    if (result == null) {
                        result = SmartList()
                    }
                    result.add(extension)
                }
            }
        }
        return when {
            result == null -> emptyList()
            result.size() > 1 -> result.toSet()
            else -> result
        }
    }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>): Collection<FunctionDescriptor> {
        return receiverTypes.flatMapTo(LinkedHashSet<FunctionDescriptor>()) { type ->
            type.memberScope.getDescriptors(DescriptorKindFilter.FUNCTIONS)
                    .filterIsInstance<FunctionDescriptor>()
                    .map { extensionForFunction(it.original) }
                    .filterNotNull()
        }
    }

    private class MyFunctionDescriptor(
            override val originalFunction: FunctionDescriptor
    ) : SamAdapterExtensionFunctionDescriptor, SimpleFunctionDescriptorImpl(
            DescriptorUtils.getContainingModule(originalFunction),
            null,
            Annotations.EMPTY, //TODO
            originalFunction.name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            originalFunction.source
    ) {
        init {
            val typeParamsSum = (originalFunction.typeParameters).toArrayList()
            val ownerClass = originalFunction.containingDeclaration as ClassDescriptor
            //TODO: should we go up parents for getters/setters too?
            for (parent in ownerClass.parentsWithSelf) {
                if (parent !is ClassDescriptor) break
                typeParamsSum += parent.typeConstructor.parameters
            }
            //TODO: duplicated parameter names

            val typeParameters = ArrayList<TypeParameterDescriptor>(typeParamsSum.size())
            val typeSubstitutor = DescriptorSubstitutor.substituteTypeParameters(typeParamsSum, TypeSubstitution.EMPTY, this, typeParameters)

            val returnType = typeSubstitutor.safeSubstitute(originalFunction.returnType!!, Variance.INVARIANT)
            val receiverType = typeSubstitutor.safeSubstitute(ownerClass.defaultType, Variance.INVARIANT)
            val valueParameters = SingleAbstractMethodUtils.createValueParametersForSamAdapter(originalFunction, this, typeSubstitutor)

            val visibility = syntheticExtensionVisibility(originalFunction)

            initialize(receiverType, null, typeParameters, valueParameters, returnType, Modality.FINAL, visibility)
        }

        override fun hasStableParameterNames() = originalFunction.hasStableParameterNames()
        override fun hasSynthesizedParameterNames() = originalFunction.hasSynthesizedParameterNames()
    }
}
