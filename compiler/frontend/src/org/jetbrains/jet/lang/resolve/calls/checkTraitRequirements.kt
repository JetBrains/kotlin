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

package org.jetbrains.jet.lang.resolve.calls

import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.lang.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.jet.lang.resolve.BindingTrace

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
    val superClass = getSuperClass(descriptor)
    result.add(superClass)
    if (!KotlinBuiltIns.getInstance().isAny(superClass)) {
        getSuperClassesReachableByClassInheritance(superClass, result)
    }
    return result
}

private fun getSuperClass(descriptor: ClassDescriptor): ClassDescriptor {
    for (supertype in descriptor.getDefaultType().getConstructor().getSupertypes()) {
        val superClassifier = supertype.getConstructor().getDeclarationDescriptor()
        if (DescriptorUtils.isClass(superClassifier) || DescriptorUtils.isEnumClass(superClassifier)) {
            return superClassifier as ClassDescriptor
        }
    }
    return KotlinBuiltIns.getInstance().getAny()
}

