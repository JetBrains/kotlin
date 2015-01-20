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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import java.util.HashMap
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.SubstitutionUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.di.InjectorForMacros
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.resolve.JetModuleUtil
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.types.TypeUtils

public object HeuristicSignatures {
    private val signatures = HashMap<Pair<FqName, Name>, List<String>>()

    ;{
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

    public fun correctedParameterType(function: FunctionDescriptor, parameterIndex: Int, moduleDescriptor: ModuleDescriptor, project: Project): JetType? {
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
            val classFqName = DescriptorUtils.getFqNameSafe(ownerClass)
            val parameterTypes = signatures[classFqName to function.getName()] ?: return null
            val typeStr = parameterTypes[parameterIndex]
            val typeParameters = ownerClass.getTypeConstructor().getParameters()

            val type = typeFromText(typeStr, typeParameters, moduleDescriptor, project)

            // now substitute type parameters with actual arguments
            val typeArgs = ownerType.getArguments()
            val typeArgsMap = typeParameters.indices.map { typeParameters[it] to typeArgs[it] }.toMap()
            val substitutor = TypeUtils.makeSubstitutorForTypeParametersMap(typeArgsMap)
            return substitutor.substitute(type, Variance.INVARIANT)
        }
    }

    private fun typeFromText(text: String, typeParameters: Collection<TypeParameterDescriptor>, moduleDescriptor: ModuleDescriptor, project: Project): JetType {
        val typeRef = JetPsiFactory(project).createType(text)
        val injector = InjectorForMacros(project, moduleDescriptor)
        val rootPackagesScope = JetModuleUtil.getSubpackagesOfRootScope(moduleDescriptor)
        val typeParametersScope = TypeParametersScope(typeParameters)
        val scope = ChainedScope(moduleDescriptor, "Root packages + type parameters", typeParametersScope, rootPackagesScope)
        val type = injector.getTypeResolver().resolveType(scope, typeRef, BindingTraceContext(), false)
        assert(!type.isError()) { "No type resolved from '$text'" }
        return type
    }

    private class TypeParametersScope(params: Collection<TypeParameterDescriptor>) : JetScope by JetScope.Empty {
        private val paramsByName = params.map { it.getName() to it }.toMap()

        override fun getClassifier(name: Name) = paramsByName[name]
    }
}