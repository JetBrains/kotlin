/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.context.ScriptContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
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
        scriptContext.scriptDescriptor.scriptEnvironmentProperties.forEach {
            propertyCodegen.generateGetter(null, it, null)
            propertyCodegen.generateSetter(null, it, null)
        }
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
        val scriptDefinition = scriptContext.script.kotlinScriptDefinition.value

        val jvmSignature = typeMapper.mapScriptSignature(
            scriptDescriptor,
            scriptContext.earlierScripts,
            scriptDefinition.implicitReceivers,
            scriptDefinition.environmentVariables
        )

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

                fun Int.incrementIf(cond: Boolean): Int = if (cond) plus(1) else this
                val valueParamStart = 1
                    .incrementIf(scriptContext.earlierScripts.isNotEmpty())
                    .incrementIf(scriptDefinition.implicitReceivers.isNotEmpty())
                    .incrementIf(scriptDefinition.environmentVariables.isNotEmpty())

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

            fun genFieldFromArrayElement(descriptor: ClassDescriptor, paramIndex: Int, elementIndex: Int, name: String) {
                val elementClassType = typeMapper.mapClass(descriptor)
                iv.load(0, classType)
                iv.load(paramIndex, elementClassType)
                iv.aconst(elementIndex)
                iv.aload(OBJECT_TYPE)
                iv.checkcast(elementClassType)
                iv.putfield(classType.internalName, name, elementClassType.descriptor)
            }

            if (!scriptContext.earlierScripts.isEmpty()) {
                val scriptsParamIndex = frameMap.enterTemp(AsmUtil.getArrayType(OBJECT_TYPE))

                scriptContext.earlierScripts.forEachIndexed { earlierScriptIndex, earlierScript ->
                    val name = scriptContext.getScriptFieldName(earlierScript)
                    genFieldFromArrayElement(earlierScript, scriptsParamIndex, earlierScriptIndex, name)
                }
            }

            if (scriptDefinition.implicitReceivers.isNotEmpty()) {
                val receiversParamIndex = frameMap.enterTemp(AsmUtil.getArrayType(OBJECT_TYPE))

                scriptContext.receiverDescriptors.forEachIndexed { receiverIndex, receiver ->
                    val name = scriptContext.getImplicitReceiverName(receiverIndex)
                    genFieldFromArrayElement(receiver, receiversParamIndex, receiverIndex, name)
                }
            }

            if (scriptDefinition.environmentVariables.isNotEmpty()) {
                val envParamIndex = frameMap.enterTemp(AsmTypes.OBJECT_TYPE)
                val mapType = PropertyCodegen.ScriptEnvPropertyAccessorStrategy.MAP_IFACE_TYPE
                iv.load(0, classType)
                iv.load(envParamIndex, mapType)
                iv.putfield(classType.internalName, PropertyCodegen.ScriptEnvPropertyAccessorStrategy.MAP_FIELD_NAME, mapType.descriptor)
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
        if (scriptContext.scriptDescriptor.scriptEnvironmentProperties.isNotEmpty()) {
            classBuilder.newField(
                NO_ORIGIN,
                ACC_PUBLIC or ACC_FINAL,
                PropertyCodegen.ScriptEnvPropertyAccessorStrategy.MAP_FIELD_NAME,
                PropertyCodegen.ScriptEnvPropertyAccessorStrategy.MAP_IFACE_TYPE.descriptor,
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
