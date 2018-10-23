/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.AsmUtil.writeAnnotationData
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.context.ScriptContext
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.Companion.NO_ORIGIN
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

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
        AnnotationCodegen.forClass(v.visitor, this, typeMapper).genAnnotations(scriptDescriptor, null)
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
        val serializer = DescriptorSerializer.create(scriptDescriptor, JvmSerializerExtension(v.serializationBindings, state), null)
        val classProto = serializer.classProto(scriptDescriptor).build()
        writeKotlinMetadata(v, state, KotlinClassHeader.Kind.CLASS, JvmAnnotationNames.METADATA_SCRIPT_FLAG) { av ->
            writeAnnotationData(av, serializer, classProto)
        }
    }

    private fun genConstructor(
            scriptDescriptor: ScriptDescriptor,
            classBuilder: ClassBuilder,
            methodContext: MethodContext
    ) {
        val scriptDefinition = scriptContext.script.kotlinScriptDefinition

        val jvmSignature = typeMapper.mapScriptSignature(
            scriptDescriptor,
            scriptContext.earlierScripts
        )
        val asmMethod = jvmSignature.asmMethod

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

        AnnotationCodegen.forMethod(mv, this, typeMapper).genAnnotations(
            scriptDescriptor.unsubstitutedPrimaryConstructor, asmMethod.returnType
        )

        if (state.classBuilderMode.generateBodies) {
            mv.visitCode()

            val iv = InstructionAdapter(mv)

            val classType = typeMapper.mapType(scriptDescriptor)

            val superclass = scriptDescriptor.getSuperClassNotAny()
            // TODO: throw if class is not found)

            val frameMap = FrameMap()
            frameMap.enterTemp(OBJECT_TYPE)

            fun genFieldFromArrayElement(descriptor: ClassDescriptor, paramIndex: Int, elementIndex: Int, name: String) {
                val elementClassType = typeMapper.mapClass(descriptor)
                val array = StackValue.local(paramIndex, AsmUtil.getArrayType(OBJECT_TYPE))
                val value = StackValue.arrayElement(OBJECT_TYPE, null, array, StackValue.constant(elementIndex, Type.INT_TYPE))
                val field = StackValue.field(elementClassType, classType, name, false, StackValue.local(0, classType))
                field.store(value, iv)
            }

            fun genFieldFromParam(fieldClassType: Type, paramIndex: Int, name: String) {
                val value = StackValue.local(paramIndex, fieldClassType)
                val field = StackValue.field(fieldClassType, classType, name, false, StackValue.local(0, classType))
                field.store(value, iv)
            }

            if (!scriptContext.earlierScripts.isEmpty()) {
                val scriptsParamIndex = frameMap.enterTemp(AsmUtil.getArrayType(OBJECT_TYPE))

                scriptContext.earlierScripts.forEachIndexed { earlierScriptIndex, earlierScript ->
                    val name = scriptContext.getScriptFieldName(earlierScript)
                    genFieldFromArrayElement(earlierScript, scriptsParamIndex, earlierScriptIndex, name)
                }
            }

            if (superclass == null) {
                iv.load(0, classType)
                iv.invokespecial("java/lang/Object", "<init>", "()V", false)
            } else {
                val ctorDesc = superclass.unsubstitutedPrimaryConstructor
                        ?: throw RuntimeException("Primary constructor not found for script template " + superclass.toString())

                iv.load(0, classType)

                val valueParameters = scriptDescriptor.unsubstitutedPrimaryConstructor.valueParameters
                for (superclassParam in ctorDesc.valueParameters) {
                    val valueParam = valueParameters.first { it.name == superclassParam.name }
                    val paramType = typeMapper.mapType(valueParam.type)
                    iv.load(valueParam!!.index + scriptContext.ctorValueParametersStart + 1, paramType)
                    frameMap.enterTemp(paramType)
                }

                val ctorMethod = typeMapper.mapToCallableMethod(ctorDesc, false)
                val sig = ctorMethod.getAsmMethod().descriptor

                iv.invokespecial(
                    typeMapper.mapSupertype(superclass.defaultType, null).internalName,
                    "<init>", sig, false
                )
            }
            iv.load(0, classType)

            scriptDescriptor.implicitReceivers.forEachIndexed { receiverIndex, receiver ->
                val receiversParamIndex = frameMap.enterTemp(AsmUtil.getArrayType(OBJECT_TYPE))
                val name = scriptContext.getImplicitReceiverName(receiverIndex)
                genFieldFromParam(typeMapper.mapClass(receiver), receiversParamIndex, name)
            }

            scriptDescriptor.scriptEnvironmentProperties.forEachIndexed { envVarIndex, envVar ->
                val fieldClassType = typeMapper.mapType(envVar)
                val envVarParamIndex = frameMap.enterTemp(fieldClassType)
                val name = scriptContext.getEnvironmentVarName(envVarIndex)
                genFieldFromParam(fieldClassType, envVarParamIndex, name)
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
            classBuilder.newField(
                NO_ORIGIN,
                ACC_PUBLIC or ACC_FINAL,
                scriptContext.getScriptFieldName(earlierScript),
                typeMapper.mapType(earlierScript).descriptor,
                null,
                null
            )
        }
        for (receiverIndex in scriptContext.receiverDescriptors.indices) {
            classBuilder.newField(
                NO_ORIGIN,
                ACC_PUBLIC or ACC_FINAL,
                scriptContext.getImplicitReceiverName(receiverIndex),
                scriptContext.getImplicitReceiverType(receiverIndex)!!.descriptor,
                null,
                null
            )
        }
        for (envVarIndex in scriptDescriptor.scriptEnvironmentProperties.indices) {
            classBuilder.newField(
                NO_ORIGIN,
                ACC_PUBLIC or ACC_FINAL,
                scriptContext.getEnvironmentVarName(envVarIndex),
                scriptContext.getEnvironmentVarType(envVarIndex).descriptor,
                null,
                null
            )
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
