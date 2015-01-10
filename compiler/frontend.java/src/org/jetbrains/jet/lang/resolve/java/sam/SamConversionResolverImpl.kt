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

package org.jetbrains.jet.lang.resolve.java.sam

import org.jetbrains.jet.lang.resolve.java.resolver.SamConversionResolver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.java.descriptor.*
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.jet.lang.resolve.java.sources.JavaSourceElement
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.types.TypeUtils
import java.util.ArrayList
import org.jetbrains.kotlin.types.checker.JetTypeChecker

public object SamConversionResolverImpl : SamConversionResolver {
    override fun resolveSamConstructor(name: Name, scope: JetScope): SamConstructorDescriptor? {
        val classifier = scope.getClassifier(name) as? LazyJavaClassDescriptor ?: return null
        if (classifier.getFunctionTypeForSamInterface() == null) return null
        return SingleAbstractMethodUtils.createSamConstructorFunction(scope.getContainingDeclaration(), classifier)
    }

    suppress("UNCHECKED_CAST")
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
    ): JetType? {
        val jClass = (classDescriptor.getSource() as? JavaSourceElement)?.javaElement as? JavaClass ?: return null
        val samInterfaceMethod = SingleAbstractMethodUtils.getSamInterfaceMethod(jClass) ?: return null
        val abstractMethod = if (jClass.getFqName() == samInterfaceMethod.getContainingClass().getFqName()) {
            resolveMethod(samInterfaceMethod)
        }
        else {
            findFunctionWithMostSpecificReturnType(TypeUtils.getAllSupertypes(classDescriptor.getDefaultType()))
        }
        return SingleAbstractMethodUtils.getFunctionTypeForAbstractMethod(abstractMethod)
    }

    private fun findFunctionWithMostSpecificReturnType(supertypes: Set<JetType>): SimpleFunctionDescriptor {
        val candidates = ArrayList<SimpleFunctionDescriptor>(supertypes.size())
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
            val candidateReturnType = candidate.getReturnType()
            val currentMostSpecificReturnType = currentMostSpecificType.getReturnType()
            assert(candidateReturnType != null && currentMostSpecificReturnType != null) { "$candidate, $currentMostSpecificReturnType" }
            if (JetTypeChecker.DEFAULT.isSubtypeOf(candidateReturnType!!, currentMostSpecificReturnType!!)) {
                currentMostSpecificType = candidate
            }
        }
        return currentMostSpecificType
    }
}
