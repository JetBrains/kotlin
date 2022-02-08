/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.extensions

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.*

class TypeAttributeTranslators(val translators: List<TypeAttributeTranslator>) {
    fun toAttributes(
        annotations: Annotations,
        typeConstructor: TypeConstructor,
        containingDeclaration: DeclarationDescriptor? = null
    ): TypeAttributes {
        val translated = translators.map { translator ->
            translator.toAttributes(annotations, typeConstructor, containingDeclaration)
        }.flatten()
        return TypeAttributes.create(translated)
    }

    fun toAnnotations(attributes: TypeAttributes): Annotations {
        val translated = translators.map { translator ->
            translator.toAnnotations(attributes)
        }.flatten()
        return Annotations.create(translated)
    }
}
