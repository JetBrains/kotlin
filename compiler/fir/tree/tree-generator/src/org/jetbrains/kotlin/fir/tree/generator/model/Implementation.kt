/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

class ImplementationWithArg(
    val implementation: Implementation,
    val argument: Importable?
) : Importable by implementation, FieldContainer by implementation {
    val element: Element get() = implementation.element
}

class Implementation(val element: Element, val name: String?) : Importable, FieldContainer {
    private val _parents = mutableListOf<ImplementationWithArg>()
    val parents: List<ImplementationWithArg> get() = _parents

    val isDefault = name == null
    override val type = name ?: element.type + "Impl"
    override val allFields = element.allFields.toMutableList().mapTo(mutableListOf()) {
        FieldWithDefault(it)
    }
    var kind: Kind = Kind.FinalClass

    override val packageName = element.packageName + ".impl"
    val usedTypes = mutableListOf<Importable>()

    init {
        if (isDefault) {
            element.defaultImplementation = this
        } else {
            element.customImplementations += this
        }
    }

    fun addParent(parent: Implementation, arg: Importable? = null) {
        _parents += ImplementationWithArg(parent, arg)
    }

    override fun get(fieldName: String): FieldWithDefault? {
        return allFields.firstOrNull { it.name == fieldName }
    }

    fun updateMutabilityAccordingParents() {
        for (parent in parents) {
            for (field in allFields) {
                val fieldFromParent = parent[field.name] ?: continue
                field.isMutable = field.isMutable || fieldFromParent.isMutable
                if (field.isMutable && field.customSetter == null) {
                    field.withGetter = false
                }
            }
        }
    }

    enum class Kind(val title: String) {
        Interface("interface"),
        FinalClass("class"),
        OpenClass("open class"),
        AbstractClass("abstract class"),
        Object("object")
    }
}