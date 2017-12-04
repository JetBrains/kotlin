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
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.Companion.NO_ORIGIN
import org.jetbrains.org.objectweb.asm.Opcodes.*

class ScriptCodegen private constructor(
        private val scriptDeclaration: KtScript,
        state: GenerationState,
        private val scriptContext: ScriptContext,
        builder: ClassBuilder
) : MemberCodegen<KtScript>(state, null, scriptContext, scriptDeclaration, builder) {
    private val scriptDescriptor = scriptContext.scriptDescriptor
    private val classAsmType = typeMapper.mapClass(scriptContext.contextDescriptor)

    override fun generateDeclaration() {
        v.defineClass(
                scriptDeclaration,
                state.classFileVersion,
                ACC_PUBLIC or ACC_SUPER,
                classAsmType.internalName,
                null,
                typeMapper.mapSupertype(scriptDescriptor.getSuperClassOrAny().defaultType, null).internalName,
                mapSupertypesNames(typeMapper, scriptDescriptor.getSuperInterfaces(), null)
        )
    }

    override fun generateBody() {
        genMembers()
        genFieldsForParameters(v)
        genConstructor(scriptDescriptor, v, scriptContext.intoFunction(scriptDescriptor.unsubstitutedPrimaryConstructor))
    }

    override fun generateSyntheticPartsBeforeBody() {
        generatePropertyMetadataArrayFieldIfNeeded(classAsmType)
    }

    override fun generateSyntheticPartsAfterBody() {}

    override fun generateKotlinMetadataAnnotation() {
        generateKotlinClassMetadataAnnotation(scriptDescriptor, true)
    }

    private fun genConstructor(
            scriptDescriptor: ScriptDescriptor,
            classBuilder: ClassBuilder,
            methodContext: MethodContext
    ) {
        val jvmSignature = typeMapper.mapScriptSignature(scriptDescriptor, scriptContext.earlierScripts)

        if (state.replSpecific.shouldGenerateScriptResultValue) {
            val resultFieldInfo = scriptContext.resultFieldInfo
            classBuilder.newField(
                    JvmDeclarationOrigin.NO_ORIGIN,
                    ACC_PUBLIC or ACC_FINAL,
                    resultFieldInfo.fieldName,
                    resultFieldInfo.fieldType.descriptor,
                    null, null)
        }

        val mv = classBuilder.newMethod(
                OtherOrigin(scriptDeclaration, scriptDescriptor.unsubstitutedPrimaryConstructor),
                ACC_PUBLIC, jvmSignature.asmMethod.name, jvmSignature.asmMethod.descriptor, null, null)

        if (state.classBuilderMode.generateBodies) {
            mv.visitCode()

            val iv = InstructionAdapter(mv)

            val classType = typeMapper.mapType(scriptDescriptor)

            val superclass = scriptDescriptor.getSuperClassNotAny()
            // TODO: throw if class is not found)

            if (superclass == null) {
                iv.load(0, classType)
                iv.invokespecial("java/lang/Object", "<init>", "()V", false)
            }
            else {
                val ctorDesc = superclass.unsubstitutedPrimaryConstructor
                               ?: throw RuntimeException("Primary constructor not found for script template " + superclass.toString())

                iv.load(0, classType)

                val valueParamStart = if (scriptContext.earlierScripts.isEmpty()) 1 else 2 // this + array of earlier scripts if not empty

                val valueParameters = scriptDescriptor.unsubstitutedPrimaryConstructor.valueParameters
                for (superclassParam in ctorDesc.valueParameters) {
                    val valueParam = valueParameters.first { it.name == superclassParam.name }
                    iv.load(valueParam!!.index + valueParamStart, typeMapper.mapType(valueParam.type))
                }

                val ctorMethod = typeMapper.mapToCallableMethod(ctorDesc, false)
                val sig = ctorMethod.getAsmMethod().descriptor

                iv.invokespecial(
                        typeMapper.mapSupertype(superclass.defaultType, null).internalName,
                        "<init>", sig, false)
            }
            iv.load(0, classType)

            val frameMap = FrameMap()
            frameMap.enterTemp(OBJECT_TYPE)


            if (!scriptContext.earlierScripts.isEmpty()) {
                val scriptsParamIndex = frameMap.enterTemp(AsmUtil.getArrayType(OBJECT_TYPE))

                var earlierScriptIndex = 0
                for (earlierScript in scriptContext.earlierScripts) {
                    val earlierClassType = typeMapper.mapClass(earlierScript)
                    iv.load(0, classType)
                    iv.load(scriptsParamIndex, earlierClassType)
                    iv.aconst(earlierScriptIndex++)
                    iv.aload(OBJECT_TYPE)
                    iv.checkcast(earlierClassType)
                    iv.putfield(classType.internalName, scriptContext.getScriptFieldName(earlierScript), earlierClassType.descriptor)
                }
            }

            val codegen = ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, methodContext, state, this)

            generateInitializers { codegen }

            iv.areturn(Type.VOID_TYPE)
        }

        mv.visitMaxs(-1, -1)
        mv.visitEnd()
    }

    private fun genFieldsForParameters(classBuilder: ClassBuilder) {
        for (earlierScript in scriptContext.earlierScripts) {
            val earlierClassName = typeMapper.mapType(earlierScript)
            val access = ACC_PUBLIC or ACC_FINAL
            classBuilder.newField(NO_ORIGIN, access, scriptContext.getScriptFieldName(earlierScript), earlierClassName.descriptor, null, null)
        }
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
        @JvmStatic
        fun createScriptCodegen(
                declaration: KtScript,
                state: GenerationState,
                parentContext: CodegenContext<*>
        ): ScriptCodegen {
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

            return ScriptCodegen(declaration, state, scriptContext, builder)
        }
    }
}
