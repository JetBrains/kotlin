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

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf

internal class ObjCExportNamer(val context: Context, val mapper: ObjCExportMapper) {
    val kotlinAnyName = "KotlinBase"

    private val methodSelectors = object : Mapping<FunctionDescriptor, String>() {
        override fun conflict(first: FunctionDescriptor, second: FunctionDescriptor): Boolean =
                !mapper.canHaveSameSelector(first, second)
    }

    private val methodSwiftNames = object : Mapping<FunctionDescriptor, String>() {
        override fun conflict(first: FunctionDescriptor, second: FunctionDescriptor): Boolean =
                !mapper.canHaveSameSelector(first, second)
        // Note: this condition is correct but can be too strict.
    }

    private val propertyNames = object : Mapping<PropertyDescriptor, String>() {
        override fun conflict(first: PropertyDescriptor, second: PropertyDescriptor): Boolean =
                !mapper.canHaveSameName(first, second)
    }

    private val classNames = object : Mapping<Any, String>() {
        override fun conflict(first: Any, second: Any): Boolean = true
    }

    private val protocolNames = object : Mapping<Any, String>() {
        override fun conflict(first: Any, second: Any): Boolean = true
    }

    fun getPackageName(fqName: FqName): String = classNames.getOrPut(fqName) {
        StringBuilder().apply {
            append(context.moduleDescriptor.namePrefix)
            if (fqName.isRoot) {
                append("TopLevel")
            } else {
                append(fqName.pathSegments().joinToString("") { it.asString().capitalize() })
            }
        }.mangledSequence { append("_") }
    }

    fun getClassOrProtocolName(descriptor: ClassDescriptor): String {
        val mapping = if (descriptor.isInterface) protocolNames else classNames

        return mapping.getOrPut(descriptor) {
            StringBuilder().apply {
                append(context.moduleDescriptor.namePrefix)

                if (descriptor.module != context.moduleDescriptor) {
                    append(descriptor.module.namePrefix)
                }

                descriptor.parentsWithSelf.takeWhile { it is ClassDescriptor }
                        .toList().reversed()
                        .joinTo(this, "") { it.name.asString().capitalize() }
            }.mangledSequence { append("_") }
        }
    }

    fun getSelector(method: FunctionDescriptor): String = methodSelectors.getOrPut(method) {
        assert(mapper.isBaseMethod(method))

        val parameters = mapper.objCValueParameters(method)

        StringBuilder().apply {
            append(method.mangledName)

            parameters.forEachIndexed { index, it ->
                val name = when {
                    it is ReceiverParameterDescriptor -> ""
                    method is PropertySetterDescriptor -> when (parameters.size) {
                        1 -> ""
                        else -> "value"
                    }
                    else -> it.name.asString()
                }

                if (index == 0) {
                    if (method is ConstructorDescriptor) append("With")
                    append(name.capitalize())
                } else {
                    append(name)
                }

                append(':')
            }
        }.mangledSequence {
            if (parameters.isNotEmpty()) {
                // "foo:" -> "foo_:"
                insert(lastIndex, '_')
            } else {
                // "foo" -> "foo_"
                append("_")
            }
        }
    }

    fun getSwiftName(method: FunctionDescriptor): String = methodSwiftNames.getOrPut(method) {
        assert(mapper.isBaseMethod(method))

        val parameters = mapper.objCValueParameters(method)
        
        StringBuilder().apply {
            append(method.mangledName)
            append("(")

            parameters.forEach {
                val label = when {
                    it is ReceiverParameterDescriptor -> "_"
                    method is PropertySetterDescriptor -> when (parameters.size) {
                        1 -> "_"
                        else -> "value"
                    }
                    else -> it.name.asString()
                }
                append(label)
                append(":")
            }

            append(")")
        }.mangledSequence {
            if (parameters.isNotEmpty()) {
                // "foo(label:)" -> "foo(label_:)"
                insert(lastIndex - 1, '_')
            } else {
                // "foo()" -> "foo_()"
                insert(lastIndex - 2, '_')
            }
        }
    }

    fun getName(property: PropertyDescriptor): String = propertyNames.getOrPut(property) {
        assert(mapper.isBaseProperty(property))
        assert(mapper.isObjCProperty(property))

        StringBuilder().apply {
            append(property.name.asString())
        }.mangledSequence {
            append('_')
        }
    }

    init {
        val any = context.builtIns.any

        classNames.forceAssign(any, kotlinAnyName)

        fun ClassDescriptor.method(name: String) =
                this.unsubstitutedMemberScope.getContributedFunctions(
                        Name.identifier(name),
                        NoLookupLocation.FROM_BACKEND
                ).single()

        val hashCode = any.method("hashCode")
        val toString = any.method("toString")
        val equals = any.method("equals")

        methodSelectors.forceAssign(hashCode, "hash")
        methodSwiftNames.forceAssign(hashCode, "hash")

        methodSelectors.forceAssign(toString, "description")
        methodSwiftNames.forceAssign(toString, "description")

        methodSelectors.forceAssign(equals, "isEqual:")
        methodSwiftNames.forceAssign(equals, "isEqual(:)")
    }

    private val FunctionDescriptor.mangledName: String get() {
        if (this is ConstructorDescriptor) {
            return "init"
        }

        val candidate = when (this) {
            is PropertyGetterDescriptor -> this.correspondingProperty.name.asString()
            is PropertySetterDescriptor -> "set${this.correspondingProperty.name.asString().capitalize()}"
            else -> this.name.asString()
        }

        val trimmedCandidate = candidate.dropWhile { it == '_' }
        for (family in listOf("alloc", "copy", "mutableCopy", "new", "init")) {
            if (trimmedCandidate.startsWithWords(family)) {
                // Then method can be detected as having special family by Objective-C compiler.
                // mangle the name:
                return "do" + candidate.capitalize()
            }
        }

        // TODO: handle clashes with NSObject methods etc.

        return candidate
    }

    private fun String.startsWithWords(words: String) = this.startsWith(words) &&
            (this.length == words.length || !this[words.length].isLowerCase())

    private abstract inner class Mapping<T : Any, N>() {
        private val elementToName = mutableMapOf<T, N>()
        private val nameToElements = mutableMapOf<N, MutableList<T>>()

        abstract fun conflict(first: T, second: T): Boolean

        fun getOrPut(element: T, nameCandidates: () -> Sequence<N>): N {
            getIfAssigned(element)?.let { return it }

            nameCandidates().forEach {
                if (tryAssign(element, it)) {
                    return it
                }
            }

            error("name candidates run out")
        }

        fun getIfAssigned(element: T): N? = elementToName[element]

        fun tryAssign(element: T, name: N): Boolean {
            if (element in elementToName) error(element)

            val elements = nameToElements.getOrPut(name) { mutableListOf() }
            if (elements.any { conflict(element, it) }) {
                return false
            }

            elements += element

            elementToName[element] = name

            return true
        }

        fun forceAssign(element: T, name: N) {
            if (name in nameToElements || element in elementToName) error(element)

            nameToElements[name] = mutableListOf(element)
            elementToName[element] = name
        }
    }

}

private inline fun StringBuilder.mangledSequence(crossinline mangle: StringBuilder.() -> Unit) =
        generateSequence(this.toString()) {
            this@mangledSequence.mangle()
            this@mangledSequence.toString()
        }

private fun ObjCExportMapper.canHaveCommonSubtype(first: ClassDescriptor, second: ClassDescriptor): Boolean {
    assert(shouldBeExposed(first))
    assert(shouldBeExposed(second))

    if (first.isSubclassOf(second) || second.isSubclassOf(first)) {
        return true
    }

    if (first.isFinalClass || second.isFinalClass) {
        return false
    }

    return first.isInterface || second.isInterface
}

private fun ObjCExportMapper.canBeInheritedBySameClass(
        first: CallableMemberDescriptor,
        second: CallableMemberDescriptor
): Boolean {
    if (this.isTopLevel(first) || this.isTopLevel(second)) {
        return (first.containingDeclaration.fqNameSafe == second.containingDeclaration.fqNameSafe)
    }

    val firstClass = this.getClassIfCategory(first) ?: first.containingDeclaration as ClassDescriptor
    val secondClass = this.getClassIfCategory(second) ?: second.containingDeclaration as ClassDescriptor

    if (first is ConstructorDescriptor) {
        return firstClass == secondClass || second !is ConstructorDescriptor && firstClass.isSubclassOf(secondClass)
    }

    if (second is ConstructorDescriptor) {
        return secondClass == firstClass || first !is ConstructorDescriptor && secondClass.isSubclassOf(firstClass)
    }

    return canHaveCommonSubtype(firstClass, secondClass)
}

private fun ObjCExportMapper.canHaveSameSelector(first: FunctionDescriptor, second: FunctionDescriptor): Boolean {
    assert(isBaseMethod(first))
    assert(isBaseMethod(second))

    if (!canBeInheritedBySameClass(first, second)) {
        return true
    }

    if (first.dispatchReceiverParameter == null || second.dispatchReceiverParameter == null) {
        // I.e. any is category method.
        return false
    }

    if (first.name != second.name) {
        return false
    }
    if (first.extensionReceiverParameter?.type != second.extensionReceiverParameter?.type) {
        return false
    }
    if (first.valueParameters.map { it.type } != second.valueParameters.map { it.type }) {
        return false
    }

    // Otherwise both are Kotlin member methods should merge in any common subclass.

    // Taking into account the conditions above, check if methods have the same ABI:
    return bridgeReturnType(first) == bridgeReturnType(second)
}

private fun ObjCExportMapper.canHaveSameName(first: PropertyDescriptor, second: PropertyDescriptor): Boolean {
    assert(isBaseProperty(first))
    assert(isObjCProperty(first))
    assert(isBaseProperty(second))
    assert(isObjCProperty(second))

    if (!canBeInheritedBySameClass(first, second)) {
        return true
    }

    if (first.dispatchReceiverParameter == null || second.dispatchReceiverParameter == null) {
        // I.e. any is category property.
        return false
    }

    return bridgePropertyType(first) == bridgePropertyType(second)
}

private val ModuleDescriptor.namePrefix: String get() {
    // <fooBar> -> FooBar
    val moduleName = this.name.asString().let { it.substring(1, it.lastIndex) }.capitalize()

    val uppers = moduleName.filterIndexed { index, character -> index == 0 || character.isUpperCase() }
    if (uppers.length >= 3) return uppers

    return moduleName
}
