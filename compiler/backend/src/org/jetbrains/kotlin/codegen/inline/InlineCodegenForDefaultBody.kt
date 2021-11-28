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

class InlineCodegenForDefaultBody(
    private val function: FunctionDescriptor,
    codegen: ExpressionCodegen,
    val state: GenerationState,
    private val jvmSignature: JvmMethodSignature,
    private val sourceCompilerForInline: PsiSourceCompilerForInline
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
        assert(!callDefault) { "inlining default stub into another default stub" }
        val (node, smap) = sourceCompilerForInline.compileInlineFunction(jvmSignature)
        val childSourceMapper = SourceMapCopier(sourceMapper, smap)

        val argsSize =
            (Type.getArgumentsAndReturnSizes(jvmSignature.asmMethod.descriptor) ushr 2) - if (callableMethod.isStaticCall()) 1 else 0
        // `$default` is only for Kotlin use so it has no `$$forInline` version - this *is* what the inliner will use.
        node.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = false)
        node.accept(object : MethodBodyVisitor(codegen.visitor) {
            // The LVT was not generated at all, so move the start of parameters to the start of the method.
            override fun visitLocalVariable(name: String, desc: String, signature: String?, start: Label, end: Label, index: Int) =
                super.visitLocalVariable(name, desc, signature, if (index < argsSize) methodStartLabel else start, end, index)

            override fun visitLineNumber(line: Int, start: Label) =
                super.visitLineNumber(childSourceMapper.mapLineNumber(line), start)
        })
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

    override fun processHiddenParameters() {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun putHiddenParamsIntoLocals() {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun reorderArgumentsIfNeeded(actualArgsWithDeclIndex: List<ArgumentAndDeclIndex>, valueParameterTypes: List<Type>) {
        throw UnsupportedOperationException("Shouldn't be called")
    }
}
