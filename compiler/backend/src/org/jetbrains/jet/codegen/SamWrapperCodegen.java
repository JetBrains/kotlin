/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.NO_FLAG_PACKAGE_PRIVATE;
import static org.jetbrains.jet.codegen.AsmUtil.genStubCode;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class SamWrapperCodegen extends GenerationStateAware {
    private static final String FUNCTION_FIELD_NAME = "function";

    @NotNull private final ClassDescriptorFromJvmBytecode samInterface;

    public SamWrapperCodegen(@NotNull GenerationState state, @NotNull ClassDescriptorFromJvmBytecode samInterface) {
        super(state);
        this.samInterface = samInterface;
    }

    public JvmClassName genWrapper(@NotNull JetFile file) {
        // Name for generated class, in form of whatever$1
        JvmClassName name = JvmClassName.byInternalName(getWrapperName(file));

        // e.g. (T, T) -> Int
        JetType functionType = samInterface.getFunctionTypeForSamInterface();
        assert functionType != null : samInterface.toString();
        // e.g. compare(T, T)
        SimpleFunctionDescriptor interfaceFunction = SingleAbstractMethodUtils.getAbstractMethodOfSamInterface(samInterface);

        ClassBuilder cv = state.getFactory().newVisitor(name.getInternalName(), file);
        cv.defineClass(file,
                       V1_6,
                       ACC_FINAL,
                       name.getInternalName(),
                       null,
                       OBJECT_TYPE.getInternalName(),
                       new String[]{JvmClassName.byClassDescriptor(samInterface).getInternalName()}
        );
        cv.visitSource(file.getName(), null);

        // e.g. ASM type for Function2
        Type functionAsmType = state.getTypeMapper().mapType(functionType, JetTypeMapperMode.VALUE);

        cv.newField(null,
                    ACC_SYNTHETIC | ACC_PRIVATE | ACC_FINAL,
                    FUNCTION_FIELD_NAME,
                    functionAsmType.getDescriptor(),
                    null,
                    null);

        generateConstructor(name.getAsmType(), functionAsmType, cv);
        generateMethod(name.getAsmType(), functionAsmType, cv, interfaceFunction, functionType);

        cv.done();

        return name;
    }

    private void generateConstructor(Type ownerType, Type functionType, ClassBuilder cv) {
        MethodVisitor mv = cv.newMethod(null, NO_FLAG_PACKAGE_PRIVATE, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, functionType), null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            genStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);

            // super constructor
            iv.load(0, OBJECT_TYPE);
            iv.invokespecial(OBJECT_TYPE.getInternalName(), "<init>", "()V");

            // save parameter to field
            iv.load(0, OBJECT_TYPE);
            iv.load(1, functionType);
            iv.putfield(ownerType.getInternalName(), FUNCTION_FIELD_NAME, functionType.getDescriptor());

            iv.visitInsn(RETURN);
            FunctionCodegen.endVisit(iv, "constructor of SAM wrapper", null);
        }
    }

    private void generateMethod(
            Type ownerType,
            Type functionType,
            ClassBuilder cv,
            SimpleFunctionDescriptor interfaceFunction,
            JetType functionJetType
    ) {

        // using static context to avoid creating ClassDescriptor and everything else
        FunctionCodegen codegen = new FunctionCodegen(CodegenContext.STATIC, cv, state);

        FunctionDescriptor invokeFunction = functionJetType.getMemberScope()
                .getFunctions(Name.identifier("invoke")).iterator().next().getOriginal();
        StackValue functionField = StackValue.field(functionType, JvmClassName.byType(ownerType), FUNCTION_FIELD_NAME, false);
        codegen.genDelegate(interfaceFunction, invokeFunction, functionField);
    }

    private String getWrapperName(@NotNull JetFile containingFile) {
        NamespaceDescriptor namespace = state.getBindingContext().get(BindingContext.FILE_TO_NAMESPACE, containingFile);
        assert namespace != null : "couldn't find namespace for file: " + containingFile.getVirtualFile();
        FqName fqName = DescriptorUtils.getFQName(namespace).toSafe();
        String packageInternalName = JvmClassName.byFqNameWithoutInnerClasses(
                PackageClassUtils.getPackageClassFqName(fqName)).getInternalName();
        return packageInternalName + "$sam$" + samInterface.getName().asString() + "$" +
               Integer.toHexString(CodegenUtil.getPathHashCode(containingFile) * 31 + DescriptorUtils.getFQName(samInterface).hashCode());
    }
}
