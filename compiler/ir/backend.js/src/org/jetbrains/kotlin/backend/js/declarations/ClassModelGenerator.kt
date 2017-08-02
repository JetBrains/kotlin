/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.js.declarations

import org.jetbrains.kotlin.backend.js.context.NamingContext
import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsClassModel
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

class ClassModelGenerator(private val naming: NamingContext, private val currentModule: ModuleDescriptor) {
    fun generateClassModel(descriptor: ClassDescriptor): JsClassModel {
        val superName = descriptor.getSuperClassNotAny()?.let { naming.innerNames[it] }
        val model = JsClassModel(naming.names[descriptor], superName)
        if (descriptor.kind != ClassKind.ANNOTATION_CLASS && !AnnotationsUtils.isNativeObject(descriptor)) {
            copyDefaultMembers(descriptor, model)
        }
        return model
    }

    private fun copyDefaultMembers(descriptor: ClassDescriptor, model: JsClassModel) {
        val members = descriptor.unsubstitutedMemberScope
                .getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                .mapNotNull { it as? CallableMemberDescriptor }

        // Traverse fake non-abstract member. Current class does not provide their implementation,
        // it can be inherited from interface.
        for (member in members.filter { !it.kind.isReal && it.modality != Modality.ABSTRACT }) {
            copySimpleMember(descriptor, member, model)
        }
    }

    private fun copySimpleMember(descriptor: ClassDescriptor, member: CallableMemberDescriptor, model: JsClassModel) {
        // Special case: fake descriptor denotes (possible multiple) private members from different super interfaces
        if (member.visibility == Visibilities.INVISIBLE_FAKE) return copyInvisibleFakeMember(descriptor, member, model)

        val memberToCopy = findMemberToCopy(member) ?: return
        val classToCopyFrom = memberToCopy.containingDeclaration as ClassDescriptor
        if (classToCopyFrom.kind != ClassKind.INTERFACE || AnnotationsUtils.isNativeObject(classToCopyFrom)) return

        val name = naming.names[member].ident
        when (member) {
            is FunctionDescriptor -> {
                copyMethod(name, name, classToCopyFrom, descriptor, model.postDeclarationBlock)
            }
            is PropertyDescriptor -> copyProperty(name, classToCopyFrom, descriptor, model.postDeclarationBlock)
        }
    }

    private fun copyInvisibleFakeMember(descriptor: ClassDescriptor, member: CallableMemberDescriptor, model: JsClassModel) {
        for (overriddenMember in member.overriddenDescriptors) {
            val memberToCopy = if (overriddenMember.kind.isReal) overriddenMember else findMemberToCopy(overriddenMember) ?: continue
            val classToCopyFrom = memberToCopy.containingDeclaration as ClassDescriptor
            if (classToCopyFrom.kind != ClassKind.INTERFACE) continue

            val name = naming.names[memberToCopy].ident
            when (member) {
                is FunctionDescriptor -> {
                    copyMethod(name, name, classToCopyFrom, descriptor, model.postDeclarationBlock)
                }
                is PropertyDescriptor -> copyProperty(name, classToCopyFrom, descriptor, model.postDeclarationBlock)
            }
        }
    }

    private fun findMemberToCopy(member: CallableMemberDescriptor): CallableMemberDescriptor? {
        // If one of overridden members is non-abstract, copy it.
        // When none found, we have nothing to copy, ignore.
        // When multiple found, our current class should provide implementation, ignore.
        val memberToCopy = member.findNonRepeatingOverriddenDescriptors({ overriddenDescriptors }, { original })
                                   .singleOrNull { it.modality != Modality.ABSTRACT } ?: return null

        // If found member is not from interface, we don't need to copy it, it's already in prototype
        if ((memberToCopy.containingDeclaration as ClassDescriptor).kind != ClassKind.INTERFACE) return null

        // If found member is fake itself, repeat search for it, until we find actual implementation
        return if (!memberToCopy.kind.isReal) findMemberToCopy(memberToCopy) else memberToCopy
    }

    private fun <T : CallableMemberDescriptor> T.findNonRepeatingOverriddenDescriptors(
            getTypedOverriddenDescriptors: T.() -> Collection<T>,
            getOriginalDescriptor: T.() -> T
    ): List<T> {
        val allDescriptors = mutableSetOf<T>()
        val repeatedDescriptors = mutableSetOf<T>()
        fun walk(descriptor: T) {
            val original = descriptor.getOriginalDescriptor()
            if (!allDescriptors.add(original)) return
            val overridden = original.getTypedOverriddenDescriptors().map { it.getOriginalDescriptor() }
            repeatedDescriptors += overridden
            overridden.forEach { walk(it) }
        }

        val directOverriddenDescriptors = getTypedOverriddenDescriptors()
        directOverriddenDescriptors.forEach { walk(it) }
        return directOverriddenDescriptors.filter { it.getOriginalDescriptor() !in repeatedDescriptors }
    }

    private fun copyMethod(
            sourceName: String,
            targetName: String,
            sourceDescriptor: ClassDescriptor,
            targetDescriptor: ClassDescriptor,
            block: JsBlock
    ) {
        if (targetDescriptor.module != currentModule) return

        val targetPrototype = buildJs { naming.innerNames[targetDescriptor].refPure().dotPrototype() }
        val sourcePrototype = buildJs { naming.innerNames[sourceDescriptor].refPure().dotPrototype() }
        block.statements += buildJs { statement(targetPrototype.dotPure(targetName).assign(sourcePrototype.dotPure(sourceName))) }
    }

    private fun copyProperty(
            name: String,
            sourceDescriptor: ClassDescriptor,
            targetDescriptor: ClassDescriptor,
            block: JsBlock
    ) {
        if (targetDescriptor.module != currentModule) return

        val targetPrototype = buildJs { naming.innerNames[targetDescriptor].refPure().dotPrototype() }
        val sourcePrototype = buildJs { naming.innerNames[sourceDescriptor].refPure().dotPrototype() }
        val getPropertyDescriptor = buildJs { "Object".dotPure("getOwnPropertyDescriptor").invoke(sourcePrototype, name.str()) }

        block.statements += buildJs { statement(targetPrototype.defineProperty(name, getPropertyDescriptor)) }
    }
}