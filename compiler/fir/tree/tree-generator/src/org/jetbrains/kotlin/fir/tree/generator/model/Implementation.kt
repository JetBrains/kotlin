/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*

class Implementation(val element: Element, val name: String?) : FieldContainer, ImplementationKindOwner {
    override val allParents: List<ImplementationKindOwner> get() = listOf(element)
    val isDefault = name == null
    override val typeName = name ?: (element.typeName + "Impl")

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

    context(ImportCollector)
    override fun renderTo(appendable: Appendable) {
        addImport(this)
        appendable.append(this.typeName)
        if (element.params.isNotEmpty()) {
            element.params.joinTo(appendable, prefix = "<", postfix = ">") { it.name }
        }
    }

    override fun substitute(map: TypeParameterSubstitutionMap) = this

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

    override val hasAcceptChildrenMethod: Boolean
        get() {
            val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
            val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass
            return !isInterface && !isAbstract
        }

    override val hasTransformChildrenMethod: Boolean
        get() = true

    override val walkableChildren: List<FieldWithDefault>
        get() = allFields.filter { it.isFirType && !it.withGetter && it.needAcceptAndTransform }

    override val transformableChildren: List<FieldWithDefault>
        get() = walkableChildren.filter { it.isMutable }

    override fun get(fieldName: String): FieldWithDefault? {
        return allFields.firstOrNull { it.name == fieldName }
    }

    val fieldsWithoutDefault by lazy { allFields.filter { it.defaultValueInImplementation == null } }
    val fieldsWithDefault by lazy { allFields.filter { it.defaultValueInImplementation != null } }

}

val ImplementationKind.hasLeafBuilder: Boolean
    get() = this == ImplementationKind.FinalClass || this == ImplementationKind.OpenClass
