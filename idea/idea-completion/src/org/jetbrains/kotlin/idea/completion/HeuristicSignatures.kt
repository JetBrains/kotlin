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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.types.IndexedParametersSubstitution
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SubstitutionUtils
import org.jetbrains.kotlin.types.Variance
import java.util.*

public class HeuristicSignatures(
        private val moduleDescriptor: ModuleDescriptor,
        private val project: Project,
        private val typeResolver: TypeResolver
) {
    public fun correctedParameterType(function: FunctionDescriptor, parameter: ValueParameterDescriptor): KotlinType? {
        val parameterIndex = function.getValueParameters().indexOf(parameter)
        assert(parameterIndex >= 0)
        return correctedParameterType(function, parameterIndex)
    }

    private fun correctedParameterType(function: FunctionDescriptor, parameterIndex: Int): KotlinType? {
        val ownerType = function.getDispatchReceiverParameter()?.getType() ?: return null

        val superFunctions = function.getOverriddenDescriptors()
        if (superFunctions.isNotEmpty()) {
            for (superFunction in superFunctions) {
                val correctedType = correctedParameterType(superFunction, parameterIndex) ?: continue
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

            val type = typeFromText(typeStr, typeParameters)

            val substitutor = IndexedParametersSubstitution(ownerClass.typeConstructor, ownerType.arguments).buildSubstitutor()
            return substitutor.substitute(type, Variance.INVARIANT)
        }
    }

    private fun typeFromText(text: String, typeParameters: Collection<TypeParameterDescriptor>): KotlinType {
        val typeRef = KtPsiFactory(project).createType(text)
        val rootPackagesScope = SubpackagesScope(moduleDescriptor, FqName.ROOT).memberScopeAsImportingScope()
        val scope = LexicalScopeImpl(rootPackagesScope, moduleDescriptor, false, null, "Root packages + type parameters") {
                        typeParameters.forEach { addClassifierDescriptor(it) }
                    }
        val type = typeResolver.resolveType(scope, typeRef, BindingTraceContext(), false)
        assert(!type.isError()) { "No type resolved from '$text'" }
        return type
    }

    companion object {
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
    }
}
