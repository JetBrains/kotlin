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

package org.jetbrains.kotlin.resolve.resolveUtil

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny

fun checkTraitRequirements(c: Map<JetClassOrObject, ClassDescriptorWithResolutionScopes>, trace: BindingTrace) {
    for ((classOrObject, descriptor) in c.entrySet()) {
        if (DescriptorUtils.isTrait(descriptor)) continue

        val satisfiedRequirements = getSuperClassesReachableByClassInheritance(descriptor)
        for (superTrait in getAllSuperTraits(descriptor)) {
            for (traitSupertype in superTrait.getDefaultType().getConstructor().getSupertypes()) {
                val traitSuperClass = traitSupertype.getConstructor().getDeclarationDescriptor()
                if (DescriptorUtils.isClass(traitSuperClass) && traitSuperClass !in satisfiedRequirements) {
                    trace.report(Errors.UNMET_TRAIT_REQUIREMENT.on(classOrObject, superTrait, traitSuperClass as ClassDescriptor))
                }
            }
        }
    }
}

private fun getAllSuperTraits(descriptor: ClassDescriptor): List<ClassDescriptor> {
    [suppress("UNCHECKED_CAST")]
    return TypeUtils.getAllSupertypes(descriptor.getDefaultType())
            .map { supertype -> supertype.getConstructor().getDeclarationDescriptor() }
            .filter { superClass -> DescriptorUtils.isTrait(superClass) } as List<ClassDescriptor>
}

private fun getSuperClassesReachableByClassInheritance(
        descriptor: ClassDescriptor,
        result: MutableSet<ClassDescriptor> = hashSetOf()
): Set<ClassDescriptor> {
    val superClass = descriptor.getSuperClassOrAny()
    result.add(superClass)
    if (!KotlinBuiltIns.isAny(superClass)) {
        getSuperClassesReachableByClassInheritance(superClass, result)
    }
    return result
}
