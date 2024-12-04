/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName

interface TypeAttributeTranslator {
    fun toAttributes(
        annotations: Annotations,
        typeConstructor: TypeConstructor? = null,
        containingDeclaration: DeclarationDescriptor? = null
    ): TypeAttributes

    fun toAnnotations(attributes: TypeAttributes): Annotations
}

object DefaultTypeAttributeTranslator : TypeAttributeTranslator {
    override fun toAttributes(
        annotations: Annotations,
        typeConstructor: TypeConstructor?,
        containingDeclaration: DeclarationDescriptor?
    ): TypeAttributes {
        return if (annotations.isEmpty())
            TypeAttributes.Empty else
            TypeAttributes.create(listOf(AnnotationsTypeAttribute(annotations)))
    }

    override fun toAnnotations(attributes: TypeAttributes): Annotations {
        return attributes.annotations
    }
}
