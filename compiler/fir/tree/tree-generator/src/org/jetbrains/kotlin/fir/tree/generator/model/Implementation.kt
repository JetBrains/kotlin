/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.fir.tree.generator.printer.generics
import org.jetbrains.kotlin.generators.tree.*

class ImplementationWithArg(
    val implementation: Implementation,
    val argument: Importable?
) : FieldContainer by implementation, ImplementationKindOwner by implementation {
    val element: Element get() = implementation.element

    override fun getTypeWithArguments(notNull: Boolean): String = type + generics
}

class Implementation(val element: Element, val name: String?) : FieldContainer, ImplementationKindOwner {
    private val _parents = mutableListOf<ImplementationWithArg>()
    val parents: List<ImplementationWithArg> get() = _parents

    override val allParents: List<ImplementationKindOwner> get() = listOf(element) + parents
    val isDefault = name == null
    override val type = name ?: element.type + "Impl"
    override val allFields = element.allFields.toMutableList().mapTo(mutableListOf()) {
        FieldWithDefault(it)
    }
    override var kind: ImplementationKind? = null
        set(value) {
            field = value
            if (kind != ImplementationKind.FinalClass) {
                isPublic = true
            }
            if (value?.hasLeafBuilder == true) {
                builder = builder ?: LeafBuilder(this)
            } else {
                builder = null
            }
        }

    override fun getTypeWithArguments(notNull: Boolean): String = type + element.generics

    override val packageName = element.packageName + ".impl"
    val usedTypes = mutableListOf<Importable>()
    val arbitraryImportables = mutableListOf<ArbitraryImportable>()

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

}

val ImplementationKind.hasLeafBuilder: Boolean
    get() = this == ImplementationKind.FinalClass || this == ImplementationKind.OpenClass
