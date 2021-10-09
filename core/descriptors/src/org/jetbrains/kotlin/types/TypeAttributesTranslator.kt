/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations

interface TypeAttributesTranslator {
    fun toAttributes(annotations: Annotations): TypeAttributes
    fun toAnnotations(attributes: TypeAttributes): Annotations?
}

object DefaultTypeAttributesTranslator : TypeAttributesTranslator {
    override fun toAnnotations(attributes: TypeAttributes): Annotations {
        val compilerAnnotations = CompilerTypeAttributes.compilerAttributeByFqName.mapNotNull { (fqName, attribute) ->
            val annotationClass = DefaultBuiltIns.Instance.getBuiltInClassByFqName(fqName)
            if (attribute in attributes)
                AnnotationDescriptorImpl(annotationClass.defaultType, mapOf(), annotationClass.source)
            else null
        }
        return Annotations.create(compilerAnnotations + attributes.customAnnotations)
    }

    override fun toAttributes(annotations: Annotations): TypeAttributes {
        val customAnnotations = mutableListOf<AnnotationDescriptor>()
        val compilerAttributes = mutableListOf<TypeAttribute<*>>()
        annotations.forEach { annotation ->
            val compilerAttribute = CompilerTypeAttributes.compilerAttributeByFqName[annotation.fqName]
            if (compilerAttribute != null)
                compilerAttributes.add(compilerAttribute)
            else
                customAnnotations.add(annotation)
        }

        return TypeAttributes.create(
            compilerAttributes + CustomAnnotationTypeAttribute(customAnnotations)
        )
    }
}
