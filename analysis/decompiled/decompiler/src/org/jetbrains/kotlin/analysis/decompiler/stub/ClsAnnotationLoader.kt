/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub

import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.AnnotationLoader
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer

/**
 * Annotation and initializer loader used by the stub builder for decompiled classes.
 *
 * Extends [AnnotationLoader] with the ability to load property initializers (compile-time constant values
 * and annotation parameter default values).
 */
interface ClsAnnotationLoader : AnnotationLoader<AnnotationWithArgs> {
    /**
     * Returns the compile-time constant initializer of a property, or `null` if the property has no constant initializer.
     */
    fun loadPropertyInitializer(container: ProtoContainer, proto: ProtoBuf.Property): ConstantValue<*>?
}
