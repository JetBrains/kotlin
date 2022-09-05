/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.extensions

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

interface ExpressionCodegenExtension {
    companion object : ProjectExtensionDescriptor<ExpressionCodegenExtension>(
            "org.jetbrains.kotlin.expressionCodegenExtension", ExpressionCodegenExtension::class.java)

    class Context(
            val codegen: ExpressionCodegen,
            val typeMapper: KotlinTypeMapper,
            val v: InstructionAdapter
    )

    /**
     *  Used for generating custom byte code for the property value obtain. This function has lazy semantics.
     *  Returns new stack value.
     */
    fun applyProperty(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: Context): StackValue? = null

    /**
     *  Used for generating custom byte code for the function call. This function has lazy semantics.
     *  Returns new stack value.
     */
    fun applyFunction(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: Context): StackValue? = null

    fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {}

    val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = false
}