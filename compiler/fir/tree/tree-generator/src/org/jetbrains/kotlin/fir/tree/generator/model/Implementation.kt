/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility

class ImplementationWithArg(
    val implementation: Implementation,
    val argument: Importable?
) : FieldContainer by implementation, KindOwner by implementation {
    val element: Element get() = implementation.element
}

class Implementation(val element: Element, val name: String?) : FieldContainer, KindOwner {
    private val _parents = mutableListOf<ImplementationWithArg>()
    val parents: List<ImplementationWithArg> get() = _parents

    override val allParents: List<KindOwner> get() = listOf(element) + parents
    val isDefault = name == null
    override val type = name ?: element.type + "Impl"
    override val allFields = element.allFields.toMutableList().mapTo(mutableListOf()) {
        FieldWithDefault(it)
    }
    override var kind: Kind? = null
        set(value) {
            field = value
            if (kind != Kind.FinalClass) {
                isPublic = true
            }
            if (value?.hasLeafBuilder == true) {
                builder = builder ?: LeafBuilder(this)
            } else {
                builder = null
            }
        }

    override val packageName = element.packageName + ".impl"
    val usedTypes = mutableListOf<Importable>()

    var isPublic = false
    var requiresOptIn = false
    var builder: LeafBuilder? = null

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

    val fieldsWithoutDefault by lazy { allFields.filter { it.defaultValueInImplementation == null } }
    val fieldsWithDefault by lazy { allFields.filter { it.defaultValueInImplementation != null } }

    enum class Kind(val title: String, val hasLeafBuilder: Boolean) {
        Interface("interface", false),
        FinalClass("class", true),
        OpenClass("open class", true),
        AbstractClass("abstract class", false),
        Object("object", false)
    }
}