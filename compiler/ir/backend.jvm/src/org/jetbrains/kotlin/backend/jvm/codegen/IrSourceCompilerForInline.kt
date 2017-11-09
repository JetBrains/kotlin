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

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.SourceInfo
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class IrSourceCompilerForInline(
        override val state: GenerationState,
        override val callElement: IrMemberAccessExpression
        ): SourceCompilerForInline {


    //TODO
    override val lookupLocation: LookupLocation
        get() = NoLookupLocation.FROM_BACKEND

    //TODO
    override val callElementText: String
        get() = callElement.toString()

    override val callsiteFile: PsiFile?
        get() = TODO("not implemented")

    override val contextKind: OwnerKind
        get() = OwnerKind.getMemberOwnerKind(callElement.descriptor.containingDeclaration)

    override val inlineCallSiteInfo: InlineCallSiteInfo
        get() = InlineCallSiteInfo("TODO", null, null)

    override val lazySourceMapper: DefaultSourceMapper
        get() = DefaultSourceMapper(SourceInfo("TODO", "TODO", 100))

    override fun generateLambdaBody(adapter: MethodVisitor, jvmMethodSignature: JvmMethodSignature, lambdaInfo: ExpressionLambda): SMAP {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun doCreateMethodNodeFromSource(callableDescriptor: FunctionDescriptor, jvmSignature: JvmMethodSignature, callDefault: Boolean, asmMethod: Method): SMAPAndMethodNode {
        TODO("not implemented")
    }

    override fun generateAndInsertFinallyBlocks(intoNode: MethodNode, insertPoints: List<MethodInliner.PointForExternalFinallyBlocks>, offsetForFinallyLocalVar: Int) {
        //TODO("not implemented")
    }

    override fun isCallInsideSameModuleAsDeclared(functionDescriptor: FunctionDescriptor): Boolean {
        //TODO("not implemented")
        return true
    }

    override fun isFinallyMarkerRequired(): Boolean {
        //TODO("not implemented")
        return false
    }

    override val compilationContextDescriptor: DeclarationDescriptor
        get() = callElement.descriptor

    override val compilationContextFunctionDescriptor: FunctionDescriptor
        get() = callElement.descriptor as FunctionDescriptor

    override fun getContextLabels(): Set<String> {
        //TODO
        return emptySet()
    }

    override fun initializeInlineFunctionContext(functionDescriptor: FunctionDescriptor) {
        //TODO
    }
}