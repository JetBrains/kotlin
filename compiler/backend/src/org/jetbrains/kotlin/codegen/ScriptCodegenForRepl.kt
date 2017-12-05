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

package org.jetbrains.kotlin.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.context.ScriptContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.Opcodes.*

class ScriptCodegenForRepl private constructor(
        private val scriptDeclaration: KtScript,
        state: GenerationState,
        private val scriptContext: ScriptContext,
        builder: ClassBuilder
) : MemberCodegen<KtScript>(state, null, scriptContext, scriptDeclaration, builder) {
    private val scriptDescriptor = scriptContext.scriptDescriptor
    private val classAsmType = typeMapper.mapClass(scriptContext.contextDescriptor)

    override fun generateDeclaration() {
        // Do not allow superclasses for REPL line scripts
        assert(scriptDescriptor.getSuperClassNotAny() == null)

        v.defineClass(
                scriptDeclaration,
                state.classFileVersion,
                ACC_PUBLIC or ACC_SUPER,
                classAsmType.internalName,
                null,
                "java/lang/Object",
                mapSupertypesNames(typeMapper, scriptDescriptor.getSuperInterfaces(), null)
        )
    }

    override fun generateBody() {
        genMembers()

        genDefaultConstructor(v)
        genRunMethod(v, scriptContext.intoFunction(scriptDescriptor.unsubstitutedPrimaryConstructor))
        genResultFieldIfNeeded(v)
    }

    override fun generateSyntheticPartsBeforeBody() {
        generatePropertyMetadataArrayFieldIfNeeded(classAsmType)
    }

    override fun generateSyntheticPartsAfterBody() {}

    override fun generateKotlinMetadataAnnotation() {
        generateKotlinClassMetadataAnnotation(scriptDescriptor, true)
    }

    private fun genResultFieldIfNeeded(classBuilder: ClassBuilder) {
        if (state.replSpecific.shouldGenerateScriptResultValue) {
            val resultFieldInfo = scriptContext.resultFieldInfo
            classBuilder.newField(
                    JvmDeclarationOrigin.NO_ORIGIN,
                    ACC_PUBLIC or ACC_FINAL or ACC_STATIC,
                    resultFieldInfo.fieldName,
                    resultFieldInfo.fieldType.descriptor,
                    null, null)
        }
    }

    private fun genDefaultConstructor(classBuilder: ClassBuilder) {
        val mv = classBuilder.newMethod(OtherOrigin(scriptDeclaration), ACC_PUBLIC, "<init>", "()V", null, null)

        if (state.classBuilderMode.generateBodies) {
            mv.visitCode()
            with (InstructionAdapter(mv)) {
                load(0, typeMapper.mapType(scriptDescriptor))
                invokespecial("java/lang/Object", "<init>", "()V", false)
                areturn(Type.VOID_TYPE)
            }
        }

        mv.visitMaxs(-1, -1)
        mv.visitEnd()
    }

    private fun genRunMethod(classBuilder: ClassBuilder, methodContext: MethodContext) {
        val mv = classBuilder.newMethod(OtherOrigin(scriptDeclaration), ACC_PUBLIC or ACC_STATIC, RUN_METHOD_NAME, "()V", null, null)

        if (state.classBuilderMode.generateBodies) {
            mv.visitCode()
            with (InstructionAdapter(mv)) {
                val codegen = ExpressionCodegen(mv, FrameMap(), Type.VOID_TYPE, methodContext, state, this@ScriptCodegenForRepl)
                generateInitializers { codegen }
                areturn(Type.VOID_TYPE)
            }
        }

        mv.visitMaxs(-1, -1)
        mv.visitEnd()
    }

    private fun genMembers() {
        for (declaration in scriptDeclaration.declarations) {
            if (declaration is KtProperty || declaration is KtNamedFunction || declaration is KtTypeAlias) {
                genSimpleMember(declaration)
            }
            else if (declaration is KtClassOrObject) {
                genClassOrObject(declaration)
            }
            else if (declaration is KtDestructuringDeclaration) {
                for (entry in declaration.entries) {
                    genSimpleMember(entry)
                }
            }
        }
    }

    companion object {
        val RUN_METHOD_NAME = "run"

        @JvmStatic
        fun createScriptCodegen(
                declaration: KtScript,
                state: GenerationState,
                parentContext: CodegenContext<*>
        ): MemberCodegen<KtScript> {
            val bindingContext = state.bindingContext
            val scriptDescriptor = bindingContext.get<PsiElement, ScriptDescriptor>(BindingContext.SCRIPT, declaration)!!

            val classType = state.typeMapper.mapType(scriptDescriptor)

            val builder = state.factory.newVisitor(
                    OtherOrigin(declaration, scriptDescriptor), classType, declaration.containingFile)

            val earlierScripts = state.replSpecific.earlierScriptsForReplInterpreter

            val scriptContext = parentContext.intoScript(
                    scriptDescriptor,
                    earlierScripts ?: emptyList(),
                    scriptDescriptor,
                    state.typeMapper
            )

            return ScriptCodegenForRepl(declaration, state, scriptContext, builder)
        }
    }
}
