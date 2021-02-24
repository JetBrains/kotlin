/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class InlineCodegenForDefaultBody(
    private val function: FunctionDescriptor,
    codegen: ExpressionCodegen,
    val state: GenerationState,
    private val methodOwner: Type,
    private val jvmSignature: JvmMethodSignature,
    private val sourceCompilerForInline: SourceCompilerForInline
) : CallGenerator {
    private val sourceMapper: SourceMapper = codegen.parentCodegen.orCreateSourceMapper

    private val methodStartLabel = linkedLabel()

    init {
        assert(InlineUtil.isInline(function)) {
            "InlineCodegenForDefaultBody can inline only inline functions: $function"
        }

        //InlineCodegenForDefaultBody created just after visitCode call
        codegen.v.visitLabel(methodStartLabel)
    }

    override fun genCallInner(callableMethod: Callable, resolvedCall: ResolvedCall<*>?, callDefault: Boolean, codegen: ExpressionCodegen) {
        val nodeAndSmap = PsiInlineCodegen(
            codegen, state, function, methodOwner, jvmSignature, TypeParameterMappings(), sourceCompilerForInline
        ).createInlineMethodNode(
            callDefault, null, codegen.typeSystem
        )
        val childSourceMapper = SourceMapCopier(sourceMapper, nodeAndSmap.classSMAP)

        val node = nodeAndSmap.node
        val transformedMethod = MethodNode(
            node.access,
            node.name,
            node.desc,
            node.signature,
            node.exceptions.toTypedArray()
        )

        val argsSize =
            (Type.getArgumentsAndReturnSizes(jvmSignature.asmMethod.descriptor) ushr 2) - if (callableMethod.isStaticCall()) 1 else 0
        node.accept(object : InlineAdapter(transformedMethod, 0, childSourceMapper) {
            override fun visitLocalVariable(name: String, desc: String, signature: String?, start: Label, end: Label, index: Int) {
                val startLabel = if (index < argsSize) methodStartLabel else start
                super.visitLocalVariable(name, desc, signature, startLabel, end, index)
            }
        })

        transformedMethod.accept(MethodBodyVisitor(codegen.visitor))
    }

    override fun genValueAndPut(
        valueParameterDescriptor: ValueParameterDescriptor?,
        argumentExpression: KtExpression,
        parameterType: JvmKotlinType,
        parameterIndex: Int
    ) {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun putValueIfNeeded(parameterType: JvmKotlinType, value: StackValue, kind: ValueKind, parameterIndex: Int) {
        //original method would be inlined directly into default impl body without any inline magic
        //so we no need to load variables on stack to further method call
    }

    override fun putCapturedValueOnStack(stackValue: StackValue, valueType: Type, paramIndex: Int) {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun processAndPutHiddenParameters(justProcess: Boolean) {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun putHiddenParamsIntoLocals() {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun reorderArgumentsIfNeeded(actualArgsWithDeclIndex: List<ArgumentAndDeclIndex>, valueParameterTypes: List<Type>) {
        throw UnsupportedOperationException("Shouldn't be called")
    }
}
