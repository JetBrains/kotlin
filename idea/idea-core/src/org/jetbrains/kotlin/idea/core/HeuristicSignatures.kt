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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES as BUILTIN_NAMES
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.SubpackagesImportingScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SubstitutionUtils
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.Variance
import java.util.*

internal class HeuristicSignatures(
        private val moduleDescriptor: ModuleDescriptor,
        private val project: Project,
        private val typeResolver: TypeResolver
) {
    fun correctedParameterType(function: FunctionDescriptor, parameter: ValueParameterDescriptor): KotlinType? {
        val parameterIndex = function.valueParameters.indexOf(parameter)
        assert(parameterIndex >= 0)
        return correctedParameterType(function, parameterIndex)
    }

    private fun correctedParameterType(function: FunctionDescriptor, parameterIndex: Int): KotlinType? {
        val ownerType = function.dispatchReceiverParameter?.type ?: return null

        val superFunctions = function.overriddenDescriptors
        if (superFunctions.isNotEmpty()) {
            for (superFunction in superFunctions) {
                val correctedType = correctedParameterType(superFunction, parameterIndex) ?: continue
                val typeSubstitutor = SubstitutionUtils.buildDeepSubstitutor(ownerType)
                return typeSubstitutor.safeSubstitute(correctedType, Variance.INVARIANT)
            }
            return null
        }
        else {
            val ownerClass = ownerType.constructor.declarationDescriptor ?: return null
            val classFqName = DescriptorUtils.getFqName(ownerClass)
            if (!classFqName.isSafe) return null
            val parameterTypes = signatures[classFqName.toSafe() to function.name] ?: return null
            val typeStr = parameterTypes[parameterIndex]
            val typeParameters = ownerClass.typeConstructor.parameters

            val type = typeFromText(typeStr, typeParameters)

            val substitutor = TypeConstructorSubstitution.create(ownerClass.typeConstructor, ownerType.arguments).buildSubstitutor()
            return substitutor.substitute(type, Variance.INVARIANT)
        }
    }

    private fun typeFromText(text: String, typeParameters: Collection<TypeParameterDescriptor>): KotlinType {
        val typeRef = KtPsiFactory(project).createType(text)
        val rootPackagesScope = SubpackagesImportingScope(null, moduleDescriptor, FqName.ROOT)
        val scope = LexicalScopeImpl(rootPackagesScope, moduleDescriptor, false, null, LexicalScopeKind.SYNTHETIC) {
            typeParameters.forEach { addClassifierDescriptor(it) }
        }
        val type = typeResolver.resolveType(scope, typeRef, BindingTraceContext(), false)
        assert(!type.isError) { "No type resolved from '$text'" }
        return type
    }

    companion object {
        private val signatures = HashMap<Pair<FqName, Name>, List<String>>()

        init {
            registerSignature(BUILTIN_NAMES.collection, "contains", "E")
            registerSignature(BUILTIN_NAMES.collection, "containsAll", BUILTIN_NAMES.collection.asString() + "<E>")
            registerSignature(BUILTIN_NAMES.mutableCollection, "remove", "E")
            registerSignature(BUILTIN_NAMES.mutableCollection, "removeAll", BUILTIN_NAMES.collection.asString() + "<E>")
            registerSignature(BUILTIN_NAMES.mutableCollection, "retainAll", BUILTIN_NAMES.collection.asString() + "<E>")
            registerSignature(BUILTIN_NAMES.list, "indexOf", "E")
            registerSignature(BUILTIN_NAMES.list, "lastIndexOf", "E")
            registerSignature(BUILTIN_NAMES.map, "get", "K")
            registerSignature(BUILTIN_NAMES.map, "containsKey", "K")
            registerSignature(BUILTIN_NAMES.map, "containsValue", "V")
            registerSignature(BUILTIN_NAMES.mutableMap, "remove", "K")
        }

        private fun registerSignature(
                classFqName: FqName,
                name: String,
                vararg parameterTypes: String) {
            signatures[classFqName to Name.identifier(name)] = parameterTypes.toList()
        }
    }
}
