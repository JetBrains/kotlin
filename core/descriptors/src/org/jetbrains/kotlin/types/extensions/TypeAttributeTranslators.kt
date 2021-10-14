/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.types.*

interface TypeAttributeTranslatorExtension : TypeAttributesTranslator

class TypeAttributeTranslators(project: Project) {
    val translators: List<TypeAttributesTranslator> =
        getInstances(project) + DefaultTypeAttributesTranslator

    fun toAttributes(annotations: Annotations): TypeAttributes {
        val translated = translators.map { translator ->
            translator.toAttributes(annotations)
        }.flatten()
        return TypeAttributes.create(translated)
    }

    fun toAnnotations(attributes: TypeAttributes): Annotations {
        val translated = translators.map { translator ->
            translator.toAnnotations(attributes)
        }.flatten()
        return Annotations.create(translated)
    }

    companion object :
        ProjectExtensionDescriptor<TypeAttributeTranslatorExtension>(
            "org.jetbrains.kotlin.extensions.typeAttribute",
            TypeAttributeTranslatorExtension::class.java
        )
}
