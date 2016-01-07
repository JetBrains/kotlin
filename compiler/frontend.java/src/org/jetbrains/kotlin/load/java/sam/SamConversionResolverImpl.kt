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

package org.jetbrains.kotlin.load.java.sam

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import java.util.*

object SamConversionResolverImpl : SamConversionResolver {
    override fun resolveSamConstructor(constructorOwner: DeclarationDescriptor, classifier: () -> ClassifierDescriptor?): SamConstructorDescriptor? {
        val classifierDescriptor = classifier()
        if (classifierDescriptor !is LazyJavaClassDescriptor || classifierDescriptor.functionTypeForSamInterface == null) return null
        return SingleAbstractMethodUtils.createSamConstructorFunction(constructorOwner, classifierDescriptor)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <D : FunctionDescriptor> resolveSamAdapter(original: D): D? {
        return when {
            !SingleAbstractMethodUtils.isSamAdapterNecessary(original) -> null
            original is JavaConstructorDescriptor -> SingleAbstractMethodUtils.createSamAdapterConstructor(original) as D
            original is JavaMethodDescriptor -> SingleAbstractMethodUtils.createSamAdapterFunction(original) as D
            else -> null
        }
    }

    override fun resolveFunctionTypeIfSamInterface(
            classDescriptor: JavaClassDescriptor,
            resolveMethod: (JavaMethod) -> FunctionDescriptor
    ): KotlinType? {
        val jClass = (classDescriptor.source as? JavaSourceElement)?.javaElement as? JavaClass ?: return null
        val samInterfaceMethod = SingleAbstractMethodUtils.getSamInterfaceMethod(jClass) ?: return null
        val abstractMethod = if (jClass.fqName == samInterfaceMethod.containingClass.fqName) {
            resolveMethod(samInterfaceMethod)
        }
        else {
            findFunctionWithMostSpecificReturnType(TypeUtils.getAllSupertypes(classDescriptor.defaultType))
        }
        return SingleAbstractMethodUtils.getFunctionTypeForAbstractMethod(abstractMethod)
    }

    private fun findFunctionWithMostSpecificReturnType(supertypes: Set<KotlinType>): SimpleFunctionDescriptor {
        val candidates = ArrayList<SimpleFunctionDescriptor>(supertypes.size)
        for (supertype in supertypes) {
            val abstractMembers = SingleAbstractMethodUtils.getAbstractMembers(supertype)
            if (!abstractMembers.isEmpty()) {
                candidates.add((abstractMembers[0] as SimpleFunctionDescriptor))
            }
        }
        if (candidates.isEmpty()) {
            throw IllegalStateException("Couldn't find abstract method in supertypes " + supertypes)
        }
        var currentMostSpecificType = candidates[0]
        for (candidate in candidates) {
            val candidateReturnType = candidate.returnType
            val currentMostSpecificReturnType = currentMostSpecificType.returnType
            assert(candidateReturnType != null && currentMostSpecificReturnType != null) { "$candidate, $currentMostSpecificReturnType" }
            if (KotlinTypeChecker.DEFAULT.isSubtypeOf(candidateReturnType!!, currentMostSpecificReturnType!!)) {
                currentMostSpecificType = candidate
            }
        }
        return currentMostSpecificType
    }
}
