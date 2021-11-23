/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.types.DefaultTypeAttributeTranslator
import org.jetbrains.kotlin.types.TypeAttributeTranslator
import org.jetbrains.kotlin.types.extensions.TypeAttributeTranslators

interface TypeAttributeTranslatorExtension : TypeAttributeTranslator {
    companion object : ProjectExtensionDescriptor<TypeAttributeTranslatorExtension>(
        "org.jetbrains.kotlin.extensions.typeAttributeTranslatorExtension",
        TypeAttributeTranslatorExtension::class.java
    ) {
        val Default = TypeAttributeTranslators(listOf(DefaultTypeAttributeTranslator))

        fun createTranslators(project: Project): TypeAttributeTranslators {
            return TypeAttributeTranslators(getInstances(project) + DefaultTypeAttributeTranslator)
        }
    }
}