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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.*;

public abstract class ClassBodyCodegen extends MemberCodegen {
    protected final JetClassOrObject myClass;
    protected final OwnerKind kind;
    protected final ClassDescriptor descriptor;
    protected final ClassBuilder v;
    protected final CodegenContext context;

    protected final List<CodeChunk> staticInitializerChunks = new ArrayList<CodeChunk>();

    protected ClassBodyCodegen(JetClassOrObject aClass, CodegenContext context, ClassBuilder v, GenerationState state) {
        super(state);
        descriptor = state.getBindingContext().get(BindingContext.CLASS, aClass);
        myClass = aClass;
        this.context = context;
        this.kind = context.getContextKind();
        this.v = v;
    }

    public final void generate() {
        generateDeclaration();

        generateClassBody();

        generateSyntheticParts();

        generateStaticInitializer();

        generateRemoveInIterator();
    }

    protected abstract void generateDeclaration();

    protected void generateSyntheticParts() {
    }

    private void generateClassBody() {
        FunctionCodegen functionCodegen = new FunctionCodegen(context, v, state);
        PropertyCodegen propertyCodegen = new PropertyCodegen(context, v, functionCodegen);

        for (JetDeclaration declaration : myClass.getDeclarations()) {
            generateDeclaration(propertyCodegen, declaration, functionCodegen);
        }

        generatePrimaryConstructorProperties(propertyCodegen, myClass);
    }

    protected void generateDeclaration(PropertyCodegen propertyCodegen, JetDeclaration declaration, FunctionCodegen functionCodegen) {
        if (declaration instanceof JetProperty || declaration instanceof JetNamedFunction) {
            genFunctionOrProperty(context, (JetTypeParameterListOwner) declaration, v);
        }
    }

    private void generatePrimaryConstructorProperties(PropertyCodegen propertyCodegen, JetClassOrObject origin) {
        boolean isAnnotation = origin instanceof JetClass && ((JetClass) origin).isAnnotation();
        for (JetParameter p : getPrimaryConstructorParameters()) {
            if (p.getValOrVarNode() != null) {
                PropertyDescriptor propertyDescriptor = state.getBindingContext().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, p);
                if (propertyDescriptor != null) {
                    if (!isAnnotation) {
                        propertyCodegen.generatePrimaryConstructorProperty(p, propertyDescriptor);
                    }
                    else {
                        Type type = state.getTypeMapper().mapType(propertyDescriptor);
                        v.newMethod(p, ACC_PUBLIC | ACC_ABSTRACT, p.getName(), "()" + type.getDescriptor(), null, null);
                    }
                }
            }
        }
    }

    protected @NotNull List<JetParameter> getPrimaryConstructorParameters() {
        if (myClass instanceof JetClass) {
            return ((JetClass) myClass).getPrimaryConstructorParameters();
        }
        return Collections.emptyList();
    }

    private void generateStaticInitializer() {
        if (staticInitializerChunks.size() > 0) {
            MethodVisitor mv = v.newMethod(null, ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
            if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
                mv.visitCode();

                InstructionAdapter v = new InstructionAdapter(mv);

                for (CodeChunk chunk : staticInitializerChunks) {
                    chunk.generate(v);
                }

                mv.visitInsn(RETURN);
                FunctionCodegen.endVisit(v, "static initializer", myClass);
            }
        }
    }

    private void generateRemoveInIterator() {
        // generates stub 'remove' function for subclasses of Iterator to be compatible with java.util.Iterator
        if (DescriptorUtils.isIteratorWithoutRemoveImpl(descriptor)) {
            MethodVisitor mv = v.getVisitor().visitMethod(ACC_PUBLIC, "remove", "()V", null, null);
            genMethodThrow(mv, "java/lang/UnsupportedOperationException", "Mutating method called on a Kotlin Iterator");
        }
    }
}
