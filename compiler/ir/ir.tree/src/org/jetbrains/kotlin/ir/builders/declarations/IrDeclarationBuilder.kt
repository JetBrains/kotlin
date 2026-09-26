/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.builders.IrElementBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.name.Name

@DeprecatedCompilerApi(
    deprecatedSince = CompilerVersionOfApiDeprecation._2_5_20,
    message = "Use IrDeclarationBuilder from org.jetbrains.kotlin.ir.declarations.builder instead (note the word order: that package, not org.jetbrains.kotlin.ir.builders.declarations).",
)
abstract class IrDeclarationBuilder : IrElementBuilder() {
    var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    lateinit var name: Name

    fun updateFrom(from: IrDeclaration) {
        super.updateFrom(from)

        origin = from.origin
        visibility = if (from is IrDeclarationWithVisibility) from.visibility else DescriptorVisibilities.PUBLIC
    }
}
