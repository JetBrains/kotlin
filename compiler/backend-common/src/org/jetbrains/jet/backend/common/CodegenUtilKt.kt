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

package org.jetbrains.jet.backend.common

import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.types.JetType
import kotlin.platform.platformStatic
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.utils.keysToMapExceptNulls
import org.jetbrains.jet.lang.resolve.DescriptorUtils

public object CodegenUtilKt {

    // class Foo : Bar by baz
    //   descriptor = Foo
    //   toTrait = Bar
    //   delegateExpressionType = typeof(baz)
    // return Map<member of Foo, corresponding member of typeOf(baz)>
    public [platformStatic] fun getDelegates(
            descriptor: ClassDescriptor,
            toTrait: ClassDescriptor,
            delegateExpressionType: JetType? = null
    ): Map<CallableMemberDescriptor, CallableDescriptor> {

        return descriptor.getDefaultType().getMemberScope().getAllDescriptors().stream()
            .filterIsInstance(javaClass<CallableMemberDescriptor>())
            .filter { it.getKind() == CallableMemberDescriptor.Kind.DELEGATION }
            .keysToMapExceptNulls {
                delegatingMember ->

                val actualDelegates = DescriptorUtils.getAllOverriddenDescriptors(delegatingMember)
                        .filter { it.getContainingDeclaration() == toTrait }
                        .map {
                            overriddenDescriptor ->
                            val scope = (delegateExpressionType ?: toTrait.getDefaultType()).getMemberScope()
                            val name = overriddenDescriptor.getName()

                            // this is the actual member of delegateExpressionType that we are delegating to
                            (scope.getFunctions(name) + scope.getProperties(name))
                                    .first {
                                        (listOf(it) + DescriptorUtils.getAllOverriddenDescriptors(it)).map { it.getOriginal() }.contains(overriddenDescriptor.getOriginal())
                                    }
                        }
                assert(actualDelegates.size <= 1) { "Meny delegates found for $delegatingMember: $actualDelegates" }

                actualDelegates.firstOrNull()
            }
    }
}