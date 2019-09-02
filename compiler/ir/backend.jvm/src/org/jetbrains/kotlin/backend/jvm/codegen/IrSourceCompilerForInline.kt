/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.codegen.BaseExpressionCodegen
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class IrSourceCompilerForInline(
    override val state: GenerationState,
    override val callElement: IrMemberAccessExpression,
    private val callee: IrFunction,
    private val codegen: ExpressionCodegen,
    private val data: BlockInfo
) : SourceCompilerForInline {

    //TODO: KotlinLookupLocation(callElement)
    override val lookupLocation: LookupLocation
        get() = NoLookupLocation.FROM_BACKEND

    override val callElementText: String
        get() = ir2string(callElement)

    override val callsiteFile: PsiFile?
        get() = codegen.context.psiSourceManager.getKtFile(codegen.irFunction.fileParent)

    override val contextKind: OwnerKind
        get() = OwnerKind.getMemberOwnerKind(callElement.descriptor.containingDeclaration)

    override val inlineCallSiteInfo: InlineCallSiteInfo
        get() {
            //TODO: support nested inline calls
            return InlineCallSiteInfo(
                codegen.classCodegen.type.internalName,
                codegen.signature.asmMethod.name,
                codegen.signature.asmMethod.descriptor,
                //compilationContextFunctionDescriptor.isInlineOrInsideInline()
                false,
                compilationContextFunctionDescriptor.isSuspend
            )
        }

    override val lazySourceMapper: DefaultSourceMapper
        get() = codegen.classCodegen.getOrCreateSourceMapper()

    private fun makeInlineNode(function: IrFunction, classCodegen: ClassCodegen, marker: CallSiteMarker?): SMAPAndMethodNode {
        var node: MethodNode? = null
        val functionCodegen = object : FunctionCodegen(function, classCodegen, isInlineLambda = marker == null) {
            override fun createMethod(flags: Int, signature: JvmMethodGenericSignature): MethodVisitor {
                val asmMethod = signature.asmMethod
                node = MethodNode(Opcodes.API_VERSION, flags, asmMethod.name, asmMethod.descriptor, signature.genericsSignature, null)
                return wrapWithMaxLocalCalc(node!!)
            }
        }
        lazySourceMapper.callSiteMarker = marker
        functionCodegen.generate()
        lazySourceMapper.callSiteMarker = null
        return SMAPAndMethodNode(node!!, SMAP(classCodegen.getOrCreateSourceMapper().resultMappings))
    }

    override fun generateLambdaBody(lambdaInfo: ExpressionLambda): SMAPAndMethodNode =
        makeInlineNode((lambdaInfo as IrExpressionLambdaImpl).function, codegen.classCodegen, null)

    override fun doCreateMethodNodeFromSource(
        callableDescriptor: FunctionDescriptor,
        jvmSignature: JvmMethodSignature,
        callDefault: Boolean,
        asmMethod: Method
    ): SMAPAndMethodNode {
        assert(callableDescriptor == callee.descriptor.original) { "Expected $callableDescriptor got ${callee.descriptor.original}" }
        assert(codegen.lastLineNumber >= 0) { "lastLineNumber shall be not negative, but is ${codegen.lastLineNumber}" }

        val irFunction = getFunctionToInline(jvmSignature, callDefault)
        return makeInlineNode(irFunction, FakeClassCodegen(irFunction, codegen.classCodegen), CallSiteMarker(codegen.lastLineNumber))
    }

    private fun getFunctionToInline(jvmSignature: JvmMethodSignature, callDefault: Boolean): IrFunction {
        val parent = callee.parentAsClass
        if (callDefault) {
            /*TODO: get rid of hack*/
            return parent.declarations.filterIsInstance<IrFunction>().single {
                it.descriptor.name.asString() == jvmSignature.asmMethod.name + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX &&
                        codegen.context.methodSignatureMapper.mapSignatureSkipGeneric(callee).asmMethod.descriptor.startsWith(
                            jvmSignature.asmMethod.descriptor.substringBeforeLast(')')
                        )
            }
        }

        if (parent.fileParent.fileEntry is MultifileFacadeFileEntry) {
            return (codegen.context.multifileFacadeMemberToPartMember[callee.symbol]
                ?: error("Function from a multi-file facade without the link to the function in the part: ${callee.render()}")).owner
        }

        return callee
    }

    override fun hasFinallyBlocks() = data.hasFinallyBlocks()

    override fun generateFinallyBlocksIfNeeded(finallyCodegen: BaseExpressionCodegen, returnType: Type, afterReturnLabel: Label) {
        require(finallyCodegen is ExpressionCodegen)
        finallyCodegen.generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data)
    }

    override fun createCodegenForExternalFinallyBlockGenerationOnNonLocalReturn(finallyNode: MethodNode, curFinallyDepth: Int) =
        ExpressionCodegen(
            codegen.irFunction, codegen.signature, codegen.frameMap, InstructionAdapter(finallyNode), codegen.classCodegen,
            codegen.isInlineLambda
        ).also {
            it.finallyDepth = curFinallyDepth
        }

    override fun isCallInsideSameModuleAsDeclared(functionDescriptor: FunctionDescriptor): Boolean {
        //TODO("not implemented")
        return true
    }

    override fun isFinallyMarkerRequired(): Boolean {
        return codegen.isFinallyMarkerRequired()
    }

    override val compilationContextDescriptor: DeclarationDescriptor
        get() = callElement.descriptor

    override val compilationContextFunctionDescriptor: FunctionDescriptor
        get() = callElement.descriptor as FunctionDescriptor

    override fun getContextLabels(): Set<String> {
        return setOf(codegen.irFunction.name.asString())
    }

    private class FakeClassCodegen(irFunction: IrFunction, codegen: ClassCodegen) :
        ClassCodegen(irFunction.parent as IrClass, codegen.context) {

        override fun createClassBuilder(): ClassBuilder {
            return FakeBuilder
        }

        companion object {
            val FakeBuilder = object : ClassBuilder {
                override fun newField(
                    origin: JvmDeclarationOrigin,
                    access: Int,
                    name: String,
                    desc: String,
                    signature: String?,
                    value: Any?
                ): FieldVisitor {
                    TODO("not implemented")
                }

                override fun newMethod(
                    origin: JvmDeclarationOrigin,
                    access: Int,
                    name: String,
                    desc: String,
                    signature: String?,
                    exceptions: Array<out String>?
                ): MethodVisitor {
                    TODO("not implemented")
                }

                override fun getSerializationBindings(): JvmSerializationBindings {
                    return JvmSerializationBindings()
                }

                override fun newAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
                    TODO("not implemented")
                }

                override fun done() {
                    TODO("not implemented")
                }

                override fun getVisitor(): ClassVisitor {
                    TODO("not implemented")
                }

                override fun defineClass(
                    origin: PsiElement?,
                    version: Int,
                    access: Int,
                    name: String,
                    signature: String?,
                    superName: String,
                    interfaces: Array<out String>
                ) {
                    TODO("not implemented")
                }

                override fun visitSource(name: String, debug: String?) {
                    TODO("not implemented")
                }

                override fun visitOuterClass(owner: String, name: String?, desc: String?) {
                    TODO("not implemented")
                }

                override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
                    TODO("not implemented")
                }

                override fun getThisName(): String {
                    TODO("not implemented")
                }

                override fun addSMAP(mapping: FileMapping?) {
                    TODO("not implemented")
                }
            }
        }
    }
}
