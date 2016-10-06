/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.context.ScriptContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class ScriptCodegen extends MemberCodegen<KtScript> {

    public static ScriptCodegen createScriptCodegen(
            @NotNull KtScript declaration,
            @NotNull GenerationState state,
            @NotNull CodegenContext parentContext
    ) {
        BindingContext bindingContext = state.getBindingContext();
        ScriptDescriptor scriptDescriptor = bindingContext.get(BindingContext.SCRIPT, declaration);
        assert scriptDescriptor != null;

        Type classType = state.getTypeMapper().mapType(scriptDescriptor);

        ClassBuilder builder = state.getFactory().newVisitor(JvmDeclarationOriginKt.OtherOrigin(declaration, scriptDescriptor),
                                                             classType, declaration.getContainingFile());
        List<ScriptDescriptor> earlierScripts = state.getReplSpecific().getEarlierScriptsForReplInterpreter();
        ScriptContext scriptContext = parentContext.intoScript(
                scriptDescriptor,
                earlierScripts == null ? Collections.<ScriptDescriptor>emptyList() : earlierScripts,
                scriptDescriptor,
                state.getTypeMapper()
        );
        return new ScriptCodegen(declaration, state, scriptContext, builder);
    }

    private final KtScript scriptDeclaration;
    private final ScriptContext context;
    private final ScriptDescriptor scriptDescriptor;
    private final Type classAsmType;

    private ScriptCodegen(
            @NotNull KtScript scriptDeclaration,
            @NotNull GenerationState state,
            @NotNull ScriptContext context,
            @NotNull ClassBuilder builder
    ) {
        super(state, null, context, scriptDeclaration, builder);
        this.scriptDeclaration = scriptDeclaration;
        this.context = context;
        this.scriptDescriptor = context.getScriptDescriptor();
        classAsmType = typeMapper.mapClass(context.getContextDescriptor());
    }

    @Override
    protected void generateDeclaration() {
        v.defineClass(scriptDeclaration,
                      state.getClassFileVersion(),
                      ACC_PUBLIC | ACC_SUPER,
                      classAsmType.getInternalName(),
                      null,
                      typeMapper.mapSupertype(DescriptorUtilsKt.getSuperClassOrAny(scriptDescriptor).getDefaultType(), null).getInternalName(),
                      CodegenUtilKt.mapSupertypesNames(typeMapper, DescriptorUtilsKt.getSuperInterfaces(scriptDescriptor), null));
    }

    @Override
    protected void generateBody() {
        genMembers();
        genFieldsForParameters(v);
        genConstructor(scriptDescriptor, v,
                       context.intoFunction(scriptDescriptor.getUnsubstitutedPrimaryConstructor()));
    }

    @Override
    protected void generateSyntheticParts() {
        generatePropertyMetadataArrayFieldIfNeeded(classAsmType);
    }

    @Override
    protected void generateKotlinMetadataAnnotation() {
        // TODO: copypaste from ImplementationBodyCodegen, so the script is seen as a KClass by reflection; implement separate kind with proper API
        generateKotlinClassMetadataAnnotation(scriptDescriptor);
    }

    private void genConstructor(
            @NotNull ScriptDescriptor scriptDescriptor,
            @NotNull ClassBuilder classBuilder,
            @NotNull MethodContext methodContext
    ) {
        JvmMethodSignature jvmSignature = typeMapper.mapScriptSignature(scriptDescriptor, context.getEarlierScripts());

        if (state.getReplSpecific().getShouldGenerateScriptResultValue()) {
            FieldInfo resultFieldInfo = context.getResultFieldInfo();
            classBuilder.newField(
                    JvmDeclarationOrigin.NO_ORIGIN,
                    ACC_PUBLIC | ACC_FINAL,
                    resultFieldInfo.getFieldName(),
                    resultFieldInfo.getFieldType().getDescriptor(),
                    null,
                    null
            );
        }

        MethodVisitor mv = classBuilder.newMethod(
                JvmDeclarationOriginKt.OtherOrigin(scriptDeclaration, scriptDescriptor.getUnsubstitutedPrimaryConstructor()),
                ACC_PUBLIC, jvmSignature.getAsmMethod().getName(), jvmSignature.getAsmMethod().getDescriptor(),
                null, null);

        if (state.getClassBuilderMode().generateBodies) {
            mv.visitCode();

            InstructionAdapter iv = new InstructionAdapter(mv);

            Type classType = typeMapper.mapType(scriptDescriptor);

            ClassDescriptor superclass = DescriptorUtilsKt.getSuperClassNotAny(scriptDescriptor);

            if (superclass == null) {
                iv.load(0, classType);
                iv.invokespecial("java/lang/Object", "<init>", "()V", false);
            }
            else {
                ConstructorDescriptor ctorDesc = superclass.getUnsubstitutedPrimaryConstructor();
                assert ctorDesc != null;

                iv.load(0, classType);

                int valueParamStart = context.getEarlierScripts().size() + 1;

                List<ValueParameterDescriptor> valueParameters = scriptDescriptor.getUnsubstitutedPrimaryConstructor().getValueParameters();
                for (ValueParameterDescriptor superclassParam: ctorDesc.getValueParameters()) {
                    ValueParameterDescriptor valueParam = null;
                    for (ValueParameterDescriptor vpd: valueParameters) {
                        if (vpd.getName().equals(superclassParam.getName())) {
                            valueParam = vpd;
                            break;
                        }
                    }
                    assert valueParam != null;
                    iv.load(valueParam.getIndex() + valueParamStart, typeMapper.mapType(valueParam.getType()));
                }

                CallableMethod ctorMethod = typeMapper.mapToCallableMethod(ctorDesc, false);
                String sig = ctorMethod.getAsmMethod().getDescriptor();

                iv.invokespecial(
                        typeMapper.mapSupertype(superclass.getDefaultType(), null).getInternalName(),
                        "<init>", sig, false);
            }
            iv.load(0, classType);

            FrameMap frameMap = new FrameMap();
            frameMap.enterTemp(OBJECT_TYPE);

            for (ScriptDescriptor importedScript : context.getEarlierScripts()) {
                frameMap.enter(importedScript, OBJECT_TYPE);
            }

            int offset = 1;

            for (ScriptDescriptor earlierScript : context.getEarlierScripts()) {
                Type earlierClassType = typeMapper.mapClass(earlierScript);
                iv.load(0, classType);
                iv.load(offset, earlierClassType);
                offset += earlierClassType.getSize();
                iv.putfield(classType.getInternalName(), context.getScriptFieldName(earlierScript), earlierClassType.getDescriptor());
            }

            final ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, methodContext, state, this);

            generateInitializers(new Function0<ExpressionCodegen>() {
                @Override
                public ExpressionCodegen invoke() {
                    return codegen;
                }
            });

            iv.areturn(Type.VOID_TYPE);
        }

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private void genFieldsForParameters(@NotNull ClassBuilder classBuilder) {
        for (ScriptDescriptor earlierScript : context.getEarlierScripts()) {
            Type earlierClassName = typeMapper.mapType(earlierScript);
            int access = ACC_PUBLIC | ACC_FINAL;
            classBuilder.newField(NO_ORIGIN, access, context.getScriptFieldName(earlierScript), earlierClassName.getDescriptor(), null, null);
        }
    }

    private void genMembers() {
        for (KtDeclaration declaration : scriptDeclaration.getDeclarations()) {
            if (declaration instanceof KtProperty || declaration instanceof KtNamedFunction) {
                genFunctionOrProperty(declaration);
            }
            else if (declaration instanceof KtClassOrObject) {
                genClassOrObject((KtClassOrObject) declaration);
            }
        }
    }
}
