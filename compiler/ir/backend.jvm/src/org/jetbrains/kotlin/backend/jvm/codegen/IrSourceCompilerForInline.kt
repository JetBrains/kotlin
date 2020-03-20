/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.codegen.BaseExpressionCodegen
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.doNotAnalyze
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.SUSPENSION_POINT_INSIDE_MONITOR
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class IrSourceCompilerForInline(
    override val state: GenerationState,
    override val callElement: IrMemberAccessExpression,
    private val callee: IrFunction,
    internal val codegen: ExpressionCodegen,
    private val data: BlockInfo
) : SourceCompilerForInline {

    override val lookupLocation: LookupLocation
        get() = object : LookupLocation {
            override val location: LocationInfo?
                get() {
                    val ktFile = codegen.classCodegen.context.psiSourceManager.getKtFile(codegen.irFunction.fileParent)
                        ?.takeUnless { it.doNotAnalyze != null } ?: return null

                    return object : LocationInfo {
                        override val filePath = ktFile.virtualFilePath

                        override val position: Position
                            get() = DiagnosticUtils.getLineAndColumnInPsiFile(
                                ktFile,
                                TextRange(callElement.startOffset, callElement.endOffset)
                            ).let { Position(it.line, it.column) }
                    }
                }
        }

    override val callElementText: String
        get() = ir2string(callElement)

    override val callsiteFile: PsiFile?
        get() = codegen.context.psiSourceManager.getKtFile(codegen.irFunction.fileParent)

    override val contextKind: OwnerKind
        get() = OwnerKind.getMemberOwnerKind(callElement.symbol.descriptor.containingDeclaration!!)

    override val inlineCallSiteInfo: InlineCallSiteInfo
        get() {
            val root = generateSequence(codegen) { it.inlinedInto }.last()
            return InlineCallSiteInfo(
                root.classCodegen.type.internalName,
                root.signature.asmMethod.name,
                root.signature.asmMethod.descriptor,
                root.irFunction.descriptor.isInlineOrInsideInline(),
                root.irFunction.isSuspend,
                findElement()?.let { CodegenUtil.getLineNumberForElement(it, false) } ?: 0
            )
        }

    override val lazySourceMapper: DefaultSourceMapper
        get() = codegen.smapOverride ?: codegen.classCodegen.getOrCreateSourceMapper()

    override fun generateLambdaBody(lambdaInfo: ExpressionLambda): SMAPAndMethodNode {
        val smap = codegen.context.getSourceMapper(codegen.classCodegen.irClass)
        val node = FunctionCodegen((lambdaInfo as IrExpressionLambdaImpl).function, codegen.classCodegen, codegen).generate(smap)
        return SMAPAndMethodNode(node, SMAP(smap.resultMappings))
    }

    override fun doCreateMethodNodeFromSource(
        callableDescriptor: FunctionDescriptor,
        jvmSignature: JvmMethodSignature,
        callDefault: Boolean,
        asmMethod: Method
    ): SMAPAndMethodNode {
        assert(callableDescriptor == callee.symbol.descriptor.original) { "Expected $callableDescriptor got ${callee.descriptor.original}" }
        // Do not inline the generated state-machine, which was generated to support java interop of inline suspend functions.
        // Instead, find its $$forInline companion (they share the same attributeOwnerId), which is generated for the inliner to use.
        val forInlineFunction = if (callee.isSuspend)
            callee.parentAsClass.functions.find {
                it.name.asString() == callee.name.asString() + FOR_INLINE_SUFFIX &&
                        it.attributeOwnerId == (callee as IrAttributeContainer).attributeOwnerId
            } ?: callee
        else
            callee
        val fakeCodegen = FakeClassCodegen(forInlineFunction, codegen.classCodegen)
        val node = FunctionCodegen(forInlineFunction, fakeCodegen).generate()
        return SMAPAndMethodNode(node, SMAP(fakeCodegen.getOrCreateSourceMapper().resultMappings))
    }

    override fun hasFinallyBlocks() = data.hasFinallyBlocks()

    override fun generateFinallyBlocksIfNeeded(finallyCodegen: BaseExpressionCodegen, returnType: Type, afterReturnLabel: Label) {
        require(finallyCodegen is ExpressionCodegen)
        finallyCodegen.generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data)
    }

    override fun createCodegenForExternalFinallyBlockGenerationOnNonLocalReturn(finallyNode: MethodNode, curFinallyDepth: Int) =
        ExpressionCodegen(
            codegen.irFunction, codegen.signature, codegen.frameMap, InstructionAdapter(finallyNode), codegen.classCodegen,
            codegen.inlinedInto, codegen.smapOverride
        ).also {
            it.finallyDepth = curFinallyDepth
        }

    override fun isCallInsideSameModuleAsDeclared(functionDescriptor: FunctionDescriptor): Boolean {
        // TODO port to IR structures
        return DescriptorUtils.areInSameModule(DescriptorUtils.getDirectMember(functionDescriptor), codegen.irFunction.descriptor)
    }

    override fun isFinallyMarkerRequired(): Boolean {
        return codegen.isFinallyMarkerRequired()
    }

    override val compilationContextDescriptor: DeclarationDescriptor
        get() = callElement.symbol.descriptor

    override val compilationContextFunctionDescriptor: FunctionDescriptor
        get() = callElement.symbol.descriptor as FunctionDescriptor

    override fun getContextLabels(): Set<String> {
        val name = codegen.irFunction.name.asString()
        return setOf(name)
    }

    // TODO: Find a way to avoid using PSI here
    override fun reportSuspensionPointInsideMonitor(stackTraceElement: String) {
        codegen.context.psiErrorBuilder
            .at(callElement.symbol.owner as IrDeclaration)
            .report(SUSPENSION_POINT_INSIDE_MONITOR, stackTraceElement)
    }

    private fun findElement() = (callElement.symbol.descriptor.original as? DeclarationDescriptorWithSource)?.source?.getPsi() as? KtElement

    internal val isPrimaryCopy: Boolean
        get() = codegen.classCodegen !is FakeClassCodegen

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
