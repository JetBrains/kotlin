/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators.fragments

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType

/**
 *  Information for compilation of code fragments for `evaluate expression`
 *
 *  The expression evaluator works by wrapping a code fragment in a method.
 *  The free variables of the fragment are closed over by the parameters of
 *  that method, and finally the method is placed in a class.
 *
 *  This data structure contains "synthesized" descriptors for that class,
 *  method and parameter lay-out.
 */
class EvaluatorFragmentInfo(
    val classDescriptor: ClassDescriptor,
    val methodDescriptor: FunctionDescriptor,
    val parameters: List<EvaluatorFragmentParameterInfo>,
    val typeArgumentsMap: Map<IrTypeParameterSymbol, IrType>
) {
    companion object {
        // Used in the IntelliJ Kotlin JVM Debugger Plug-In (CodeFragmentCompiler)
        // TODO: Remove once intellij-community#1839 has landed.
        @Suppress("unused")
        fun createWithFragmentParameterInfo(
            classDescriptor: ClassDescriptor,
            methodDescriptor: FunctionDescriptor,
            parametersWithInfo: List<EvaluatorFragmentParameterInfo>
        ) =
            EvaluatorFragmentInfo(classDescriptor, methodDescriptor, parametersWithInfo, emptyMap())
    }
}

data class EvaluatorFragmentParameterInfo(
    val descriptor: DeclarationDescriptor,
    val isLValue: Boolean,
)