/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators.fragments

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor

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
    val parameters: List<DeclarationDescriptor>
)