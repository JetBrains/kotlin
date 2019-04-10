/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
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
    private val codegen: ExpressionCodegen,
    private val data: BlockInfo
) : SourceCompilerForInline {


    //TODO
    override val lookupLocation: LookupLocation
        get() = NoLookupLocation.FROM_BACKEND

    //TODO
    override val callElementText: String
        get() = callElement.toString()

    //TODO
    override val callsiteFile: PsiFile?
        get() = callElement.descriptor.psiElement?.containingFile

    override val contextKind: OwnerKind
        get() = OwnerKind.getMemberOwnerKind(callElement.descriptor.containingDeclaration)

    override val inlineCallSiteInfo: InlineCallSiteInfo
        get() = InlineCallSiteInfo("TODO", null, null, false)

    override val lazySourceMapper: DefaultSourceMapper
        get() = codegen.classCodegen.getOrCreateSourceMapper()

    override fun generateLambdaBody(adapter: MethodVisitor, jvmMethodSignature: JvmMethodSignature, lambdaInfo: ExpressionLambda): SMAP {
        lambdaInfo as? IrExpressionLambdaImpl ?: error("Expecting ir lambda, but $lambdaInfo")

        val functionCodegen = object : FunctionCodegen(lambdaInfo.function, codegen.classCodegen, true) {
            override fun createMethod(flags: Int, signature: JvmMethodGenericSignature): MethodVisitor {
                return adapter
            }
        }
        functionCodegen.generate()

        return SMAP(codegen.classCodegen.getOrCreateSourceMapper().resultMappings)
    }

    override fun doCreateMethodNodeFromSource(
        callableDescriptor: FunctionDescriptor,
        jvmSignature: JvmMethodSignature,
        callDefault: Boolean,
        asmMethod: Method
    ): SMAPAndMethodNode {
        assert(callableDescriptor == callElement.descriptor.original)
        val irFunction = (callElement as IrCall).symbol.owner.let { irFunction ->
            if (!callDefault) irFunction
            else {
                /*TODO: get rid of hack*/
                val parent = irFunction.parent
                val irClass = if (parent is IrFile) parent.declarations.filterIsInstance<IrClass>().single {
                    //find class for package part
                    it.thisReceiver == null
                }
                else parent as IrClass

                irClass.declarations.filterIsInstance<IrFunction>().single {
                    it.descriptor.name.asString() == jvmSignature.asmMethod.name + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX &&
                            state.typeMapper.mapSignatureSkipGeneric(callableDescriptor).asmMethod.descriptor.startsWith(
                                jvmSignature.asmMethod.descriptor.substringBeforeLast(')')
                            )
                }
            }
        }

        //ExpressionCodegen()
        var node: MethodNode? = null
        var maxCalcAdapter: MethodVisitor? = null
        val fakeClassCodegen = FakeClassCodegen(irFunction, codegen.classCodegen)
        val functionCodegen = object : FunctionCodegen(irFunction, fakeClassCodegen) {
            override fun createMethod(flags: Int, signature: JvmMethodGenericSignature): MethodVisitor {
                node = MethodNode(
                    Opcodes.API_VERSION,
                    flags,
                    signature.asmMethod.name, signature.asmMethod.descriptor,
                    signature.genericsSignature, null
                )
                maxCalcAdapter = wrapWithMaxLocalCalc(node!!)
                return maxCalcAdapter!!
            }
        }

        assert(codegen.lastLineNumber >= 0)
        lazySourceMapper.callSiteMarker = CallSiteMarker(codegen.lastLineNumber)
        functionCodegen.generate()
        lazySourceMapper.callSiteMarker = null
        maxCalcAdapter!!.visitMaxs(-1, -1)
        maxCalcAdapter!!.visitEnd()

        return SMAPAndMethodNode(node!!, SMAP(fakeClassCodegen.getOrCreateSourceMapper().resultMappings))
    }

    override fun hasFinallyBlocks() = data.hasFinallyBlocks()

    override fun generateFinallyBlocksIfNeeded(finallyCodegen: BaseExpressionCodegen, returnType: Type, afterReturnLabel: Label) {
        require(finallyCodegen is ExpressionCodegen)
        finallyCodegen.generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data)
    }

    override fun createCodegenForExternalFinallyBlockGenerationOnNonLocalReturn(finallyNode: MethodNode, curFinallyDepth: Int) =
        ExpressionCodegen(
            codegen.irFunction, codegen.frameMap, InstructionAdapter(finallyNode), codegen.classCodegen, codegen.isInlineLambda
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

    override fun initializeInlineFunctionContext(functionDescriptor: FunctionDescriptor) {
        //TODO
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