/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.extensions

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode

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

    /**
     * Called when inliner encounters [ReifiedTypeInliner.OperationKind.PLUGIN_DEFINED] marker
     * to perform extension-specific operation with reified type parameter.
     *
     * @return Required stack size for method after inlining is performed, 0 if the size is unknown, or -1 if extension ignores this marker
     */
    fun applyPluginDefinedReifiedOperationMarker(
        insn: MethodInsnNode,
        instructions: InsnList,
        type: KotlinType,
        asmType: Type,
        typeMapper: KotlinTypeMapper,
        typeSystem: TypeSystemCommonBackendContext,
        module: ModuleDescriptor
    ): Int = -1

    fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {}

    val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = false
}