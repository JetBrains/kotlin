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

package org.jetbrains.kotlin.idea.completion

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.SubpackagesScope
import org.jetbrains.kotlin.frontend.di.createContainerForMacros
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.LookupLocation
import org.jetbrains.kotlin.types.*
import java.util.HashMap

public object HeuristicSignatures {
    private val signatures = HashMap<Pair<FqName, Name>, List<String>>()

    init {
        registerSignature("kotlin.Collection", "contains", "E")
        registerSignature("kotlin.Collection", "containsAll", "kotlin.Collection<E>")
        registerSignature("kotlin.MutableCollection", "remove", "E")
        registerSignature("kotlin.MutableCollection", "removeAll", "kotlin.Collection<E>")
        registerSignature("kotlin.MutableCollection", "retainAll", "kotlin.Collection<E>")
        registerSignature("kotlin.List", "indexOf", "E")
        registerSignature("kotlin.List", "lastIndexOf", "E")
        registerSignature("kotlin.Map", "get", "K")
        registerSignature("kotlin.Map", "containsKey", "K")
        registerSignature("kotlin.Map", "containsValue", "V")
        registerSignature("kotlin.MutableMap", "remove", "K")
    }

    private fun registerSignature(
            classFqName: String,
            name: String,
            vararg parameterTypes: String) {
        signatures[FqName(classFqName) to Name.identifier(name)] = parameterTypes.toList()
    }

    public fun correctedParameterType(function: FunctionDescriptor, parameter: ValueParameterDescriptor, moduleDescriptor: ModuleDescriptor, project: Project): JetType? {
        val parameterIndex = function.getValueParameters().indexOf(parameter)
        assert(parameterIndex >= 0)
        return correctedParameterType(function, parameterIndex, moduleDescriptor, project)
    }

    private fun correctedParameterType(function: FunctionDescriptor, parameterIndex: Int, moduleDescriptor: ModuleDescriptor, project: Project): JetType? {
        val ownerType = function.getDispatchReceiverParameter()?.getType() ?: return null

        val superFunctions = function.getOverriddenDescriptors()
        if (superFunctions.isNotEmpty()) {
            for (superFunction in superFunctions) {
                val correctedType = correctedParameterType(superFunction, parameterIndex, moduleDescriptor, project) ?: continue
                val typeSubstitutor = SubstitutionUtils.buildDeepSubstitutor(ownerType)
                return typeSubstitutor.safeSubstitute(correctedType, Variance.INVARIANT)
            }
            return null
        }
        else {
            val ownerClass = ownerType.getConstructor().getDeclarationDescriptor() ?: return null
            val classFqName = DescriptorUtils.getFqName(ownerClass)
            if (!classFqName.isSafe()) return null
            val parameterTypes = signatures[classFqName.toSafe() to function.getName()] ?: return null
            val typeStr = parameterTypes[parameterIndex]
            val typeParameters = ownerClass.getTypeConstructor().getParameters()

            val type = typeFromText(typeStr, typeParameters, moduleDescriptor, project)

            val substitutor = IndexedParametersSubstitution(ownerClass.typeConstructor, ownerType.arguments).buildSubstitutor()
            return substitutor.substitute(type, Variance.INVARIANT)
        }
    }

    private fun typeFromText(text: String, typeParameters: Collection<TypeParameterDescriptor>, moduleDescriptor: ModuleDescriptor, project: Project): JetType {
        val typeRef = JetPsiFactory(project).createType(text)
        val container = createContainerForMacros(project, moduleDescriptor)
        val rootPackagesScope = SubpackagesScope(moduleDescriptor, FqName.ROOT)
        val typeParametersScope = TypeParametersScope(typeParameters)
        val scope = ChainedScope(moduleDescriptor, "Root packages + type parameters", typeParametersScope, rootPackagesScope)
        val type = container.typeResolver.resolveType(scope, typeRef, BindingTraceContext(), false)
        assert(!type.isError()) { "No type resolved from '$text'" }
        return type
    }

    private class TypeParametersScope(params: Collection<TypeParameterDescriptor>) : JetScope by JetScope.Empty {
        private val paramsByName = params.map { it.getName() to it }.toMap()

        override fun getClassifier(name: Name, location: LookupLocation) = paramsByName[name]
    }
}
