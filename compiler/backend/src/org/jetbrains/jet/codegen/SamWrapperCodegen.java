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
import org.jetbrains.jet.lang.resolve.kotlin.PackagePartClassUtils;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import static org.jetbrains.jet.codegen.AsmUtil.NO_FLAG_PACKAGE_PRIVATE;
import static org.jetbrains.jet.codegen.AsmUtil.writeKotlinSyntheticClassAnnotation;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KotlinSyntheticClass;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class SamWrapperCodegen {
    private static final String FUNCTION_FIELD_NAME = "function";

    private final GenerationState state;
    private final JetTypeMapper typeMapper;
    private final SamType samType;

    public SamWrapperCodegen(@NotNull GenerationState state, @NotNull SamType samType) {
        this.state = state;
        this.typeMapper = state.getTypeMapper();
        this.samType = samType;
    }

    public Type genWrapper(@NotNull JetFile file) {
        // Name for generated class, in form of whatever$1
        Type asmType = Type.getObjectType(getWrapperName(file));

        // e.g. (T, T) -> Int
        JetType functionType = samType.getKotlinFunctionType();
        // e.g. compare(T, T)
        SimpleFunctionDescriptor erasedInterfaceFunction = samType.getAbstractMethod().getOriginal();

        ClassBuilder cv = state.getFactory().newVisitor(asmType, file);
        cv.defineClass(file,
                       V1_6,
                       ACC_FINAL,
                       asmType.getInternalName(),
                       null,
                       OBJECT_TYPE.getInternalName(),
                       new String[]{ typeMapper.mapType(samType.getType()).getInternalName() }
        );
        cv.visitSource(file.getName(), null);

        writeKotlinSyntheticClassAnnotation(cv, KotlinSyntheticClass.Kind.SAM_WRAPPER);

        // e.g. ASM type for Function2
        Type functionAsmType = typeMapper.mapType(functionType);

        cv.newField(null,
                    ACC_SYNTHETIC | ACC_PRIVATE | ACC_FINAL,
                    FUNCTION_FIELD_NAME,
                    functionAsmType.getDescriptor(),
                    null,
                    null);

        generateConstructor(asmType, functionAsmType, cv);
        generateMethod(asmType, functionAsmType, cv, erasedInterfaceFunction, functionType);

        cv.done();

        return asmType;
    }

    private void generateConstructor(Type ownerType, Type functionType, ClassBuilder cv) {
        MethodVisitor mv = cv.newMethod(null, NO_FLAG_PACKAGE_PRIVATE, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, functionType), null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
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
            SimpleFunctionDescriptor erasedInterfaceFunction,
            JetType functionJetType
    ) {
        // using static context to avoid creating ClassDescriptor and everything else
        FunctionCodegen codegen = new FunctionCodegen(CodegenContext.STATIC, cv, state, null);

        FunctionDescriptor invokeFunction = functionJetType.getMemberScope()
                .getFunctions(Name.identifier("invoke")).iterator().next().getOriginal();
        StackValue functionField = StackValue.field(functionType, ownerType, FUNCTION_FIELD_NAME, false);
        codegen.genDelegate(erasedInterfaceFunction, invokeFunction, functionField);
    }

    @NotNull
    private String getWrapperName(@NotNull JetFile containingFile) {
        FqName packageClassFqName = PackageClassUtils.getPackageClassFqName(containingFile.getPackageFqName());
        String packageInternalName = AsmUtil.internalNameByFqNameWithoutInnerClasses(packageClassFqName);
        JavaClassDescriptor descriptor = samType.getJavaClassDescriptor();
        return packageInternalName + "$sam$" + descriptor.getName().asString() + "$" +
               Integer.toHexString(
                       PackagePartClassUtils.getPathHashCode(containingFile.getVirtualFile()) * 31 +
                       DescriptorUtils.getFqNameSafe(descriptor).hashCode()
               );
    }
}
