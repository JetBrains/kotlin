/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.annotations.Annotations

interface TypeAttributesTranslator {
    fun toAttributes(annotations: Annotations): TypeAttributes
    fun toAnnotations(attributes: TypeAttributes): Annotations
}

object DefaultTypeAttributesTranslator : TypeAttributesTranslator {
    override fun toAnnotations(attributes: TypeAttributes): Annotations {
        return attributes.customAnnotations
    }

    override fun toAttributes(annotations: Annotations): TypeAttributes {
        return TypeAttributes.create(listOf(CustomAnnotationTypeAttribute(annotations)))
    }
}
