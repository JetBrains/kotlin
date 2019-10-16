/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions

class JsGeneratorExtensions : GeneratorExtensions() {
    override fun computeFieldVisibility(descriptor: PropertyDescriptor): Visibility =
        if (descriptor.annotations.hasAnnotation(JS_EXPORT_FQ_NAME))
            descriptor.visibility
        else
            Visibilities.PRIVATE


    companion object {
        val JS_EXPORT_FQ_NAME = FqName("kotlin.js.JsExport")
    }
}
