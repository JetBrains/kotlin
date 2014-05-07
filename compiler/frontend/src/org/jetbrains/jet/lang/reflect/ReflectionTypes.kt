/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.reflect

import kotlin.properties.Delegates
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.JetTypeImpl

public class ReflectionTypes(private val module: ModuleDescriptor) {
    private val kotlinReflect: JetScope by Delegates.lazy {
        // TODO: handle errors gracefully (error types)
        val kotlin = module.getPackage(FqName("kotlin")) ?: error("Package kotlin not found in $module")
        val reflect = kotlin.getMemberScope().getPackage(Name.identifier("reflect")) ?: error("Package reflect not found in $kotlin")
        reflect.getMemberScope()
    }

    fun find(className: String): ClassDescriptor {
        val name = Name.identifier(className)
        return kotlinReflect.getClassifier(name) as? ClassDescriptor ?: error("Reflection class not found: $name")
    }

    public fun getKFunction(n: Int): ClassDescriptor = find("KFunction$n")
    public fun getKExtensionFunction(n: Int): ClassDescriptor = find("KExtensionFunction$n")
    public fun getKMemberFunction(n: Int): ClassDescriptor = find("KMemberFunction$n")

    public fun getKFunctionType(
            annotations: Annotations,
            receiverType: JetType?,
            parameterTypes: List<JetType>,
            returnType: JetType,
            extensionFunction: Boolean
    ): JetType {
        val arguments = KotlinBuiltIns.getFunctionTypeArgumentProjections(receiverType, parameterTypes, returnType)
        val classDescriptor = correspondingKFunctionClass(receiverType, extensionFunction, parameterTypes.size)

        return JetTypeImpl(annotations, classDescriptor.getTypeConstructor(), false, arguments, classDescriptor.getMemberScope(arguments))
    }

    private fun correspondingKFunctionClass(
            receiverType: JetType?,
            extensionFunction: Boolean,
            numberOfParameters: Int
    ): ClassDescriptor {
        if (extensionFunction) {
            return getKExtensionFunction(numberOfParameters)
        }
        if (receiverType != null) {
            return getKMemberFunction(numberOfParameters)
        }
        return getKFunction(numberOfParameters)
    }
}
