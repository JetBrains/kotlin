/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

abstract class TransformingTestMethodModel(val source: SimpleTestMethodModel, val transformer: String) : MethodModel {
    override val kind: MethodModel.Kind
        get() = Kind
    abstract override val name: String
    override val dataString: String
        get() = source.dataString
    override val tags: List<String>
        get() = source.tags

    object Kind : MethodModel.Kind()
}