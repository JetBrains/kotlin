/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.constant.KClassValue
import org.jetbrains.kotlin.name.ClassId

/**
 * A component which can provide [ClassId] of the class for local class references in constant expressions and annotation arguments.
 *
 * This is used, for example, to serialize annotations with local class references to metadata on JVM. In this case, annotation arguments
 * are being evaluated by the IR expression evaluator, which stores which IrClass is referenced in a `::class` literal. Later, JVM backend
 * computes JVM internal names for every local class in the source code, and during serialization, it can provide the correct JVM name
 * to the annotation serializer.
 */
abstract class LocalClassIdOracle {
    abstract fun getLocalClassId(klass: KClassValue.Value.LocalClass): ClassId?

    companion object {
        val EMPTY: LocalClassIdOracle = object : LocalClassIdOracle() {
            override fun getLocalClassId(klass: KClassValue.Value.LocalClass): ClassId? = null
        }
    }
}
