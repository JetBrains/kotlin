/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

class WithoutJvmInlineTestMethodModel(
    source: SimpleTestMethodModel,
    val withAnnotation: Boolean
) : TransformingTestMethodModel(
    source,
    transformer = "s -> s.replaceAll(\"OPTIONAL_JVM_INLINE_ANNOTATION\", \"${if (withAnnotation) "@kotlin.jvm.JvmInline" else ""}\")"
) {
    override val name: String = source.name + if (withAnnotation) "" else "_valueClasses"
}