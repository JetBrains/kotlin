/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

import org.jetbrains.kotlin.test.TargetBackend

abstract class TransformingTestMethodModel(val source: SimpleTestMethodModel, val transformer: String) : MethodModel {
    override val kind: MethodModel.Kind
        get() = Kind
    abstract override val name: String
    override val dataString: String
        get() = source.dataString
    override val tags: List<String>
        get() = source.tags

    object TransformerFunctionsClassPlaceHolder
    object Kind : MethodModel.Kind()

    override fun imports(): Collection<Class<*>> = super.imports() + TransformerFunctionsClassPlaceHolder::class.java

    internal val isNative
        get() = source.targetBackend in listOf(TargetBackend.NATIVE, TargetBackend.ANY)
    // Native tests load sources before runTest call if more than 1 test is called, so we need to register it before.
    // Existing native tests specify target backend as ANY, setting it to NATIVE removes some previously generated tests.
}