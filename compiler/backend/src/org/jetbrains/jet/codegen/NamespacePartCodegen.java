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

import com.google.common.collect.Lists;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses;

public class NamespacePartCodegen extends MemberCodegen {

    private final ClassBuilder v;

    private final PackageFragmentDescriptor descriptor;

    private final JetFile jetFile;

    private final Type namespacePartName;

    private final FieldOwnerContext context;

    public NamespacePartCodegen(
            @NotNull ClassBuilder v,
            @NotNull JetFile jetFile,
            @NotNull Type namespacePartName,
            @NotNull FieldOwnerContext context,
            @NotNull GenerationState state
    ) {
        super(state, null);
        this.v = v;
        this.jetFile = jetFile;
        this.namespacePartName = namespacePartName;
        this.context = context;
        descriptor = state.getBindingContext().get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, jetFile);
        assert descriptor != null : "No namespace found for jetFile " + jetFile + " declared package: " + jetFile.getPackageName();
    }

    public void generate() {
        v.defineClass(jetFile, V1_6,
                      ACC_PUBLIC | ACC_FINAL,
                      namespacePartName.getInternalName(),
                      null,
                      "java/lang/Object",
                      ArrayUtil.EMPTY_STRING_ARRAY
        );
        v.visitSource(jetFile.getName(), null);

        writeKotlinPackageFragmentAnnotation();

        for (JetDeclaration declaration : jetFile.getDeclarations()) {
            if (declaration instanceof JetNamedFunction || declaration instanceof JetProperty) {
                genFunctionOrProperty(context, (JetTypeParameterListOwner) declaration, v);
            }
        }

        generateStaticInitializers();

        v.done();
    }

    private void writeKotlinPackageFragmentAnnotation() {
        AnnotationVisitor av = v.newAnnotation(asmDescByFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_PACKAGE_FRAGMENT), true);
        av.visit(JvmAnnotationNames.ABI_VERSION_FIELD_NAME, JvmAbi.VERSION);
        av.visitEnd();
    }


    private void generateStaticInitializers() {
        List<JetProperty> properties = collectPropertiesToInitialize();
        if (properties.isEmpty()) return;

        MethodVisitor mv = v.newMethod(jetFile, ACC_STATIC, "<clinit>", "()V", null, null);
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();

            FrameMap frameMap = new FrameMap();

            SimpleFunctionDescriptorImpl clInit =
                    new SimpleFunctionDescriptorImpl(this.descriptor, Collections.<AnnotationDescriptor>emptyList(),
                                                     Name.special("<clinit>"),
                                                     CallableMemberDescriptor.Kind.SYNTHESIZED);
            clInit.initialize(null, null, Collections.<TypeParameterDescriptor>emptyList(),
                              Collections.<ValueParameterDescriptor>emptyList(), null, null, Visibilities.PRIVATE);

            ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, this.context.intoFunction(clInit), state, this);

            for (JetDeclaration declaration : properties) {
                ImplementationBodyCodegen.
                        initializeProperty(codegen, state.getBindingContext(), (JetProperty) declaration);
            }

            mv.visitInsn(RETURN);
            FunctionCodegen.endVisit(mv, "static initializer for namespace", jetFile);
            mv.visitEnd();
        }
    }

    @NotNull
    private List<JetProperty> collectPropertiesToInitialize() {
        List<JetProperty> result = Lists.newArrayList();
        for (JetDeclaration declaration : jetFile.getDeclarations()) {
            if (declaration instanceof JetProperty &&
                ImplementationBodyCodegen.shouldInitializeProperty((JetProperty) declaration, typeMapper)) {
                result.add((JetProperty) declaration);
            }
        }
        return result;
    }
}
