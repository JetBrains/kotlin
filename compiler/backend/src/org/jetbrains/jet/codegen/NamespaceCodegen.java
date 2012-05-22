/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author max
 */
public class NamespaceCodegen {
    private final ClassBuilder v;
    private final GenerationState state;

    public NamespaceCodegen(ClassBuilder v, @NotNull FqName fqName, GenerationState state, PsiFile sourceFile) {
        this.v = v;
        this.state = state;

        v.defineClass(sourceFile, V1_6,
                      ACC_PUBLIC/*|ACC_SUPER*/,
                      getJVMClassNameForKotlinNs(fqName).getInternalName(),
                      null,
                      //"jet/lang/Namespace",
                      "java/lang/Object",
                      new String[0]
        );
        // TODO figure something out for a namespace that spans multiple files
        v.visitSource(sourceFile.getName(), null);
    }

    public void generate(JetFile file) {
        NamespaceDescriptor descriptor = state.getBindingContext().get(BindingContext.FILE_TO_NAMESPACE, file);
        final CodegenContext context = CodegenContext.STATIC.intoNamespace(descriptor);

        final FunctionCodegen functionCodegen = new FunctionCodegen(context, v, state);
        final PropertyCodegen propertyCodegen = new PropertyCodegen(context, v, functionCodegen, state);

        for (JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                propertyCodegen.gen((JetProperty) declaration);
            }
            else if (declaration instanceof JetNamedFunction) {
                try {
                    functionCodegen.gen((JetNamedFunction) declaration);
                }
                catch (CompilationException e) {
                    throw e;
                }
                catch (Exception e) {
                    throw new CompilationException("Failed to generate function " + declaration.getName(), e, declaration);
                }
            }
            else if (declaration instanceof JetClassOrObject) {
                state.getInjector().getClassCodegen().generate(context, (JetClassOrObject) declaration);
            }
            else if (declaration instanceof JetScript) {
                state.getInjector().getScriptCodegen().generate(context, (JetScript) declaration);
            }
//            else if (declaration instanceof JetFile) {
//                JetFile childNamespace = (JetFile) declaration;
//                state.forNamespace(childNamespace).generate(childNamespace);
//            }
        }

        if (hasNonConstantPropertyInitializers(file)) {
            generateStaticInitializers(file);
        }
    }

    private void generateStaticInitializers(JetFile namespace) {
        MethodVisitor mv = v.newMethod(namespace, ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();

            FrameMap frameMap = new FrameMap();
            ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, CodegenContext.STATIC, state);

            for (JetDeclaration declaration : namespace.getDeclarations()) {
                if (declaration instanceof JetProperty) {
                    final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                    if (initializer != null && !(initializer instanceof JetConstantExpression)) {
                        final PropertyDescriptor descriptor = (PropertyDescriptor) state.getBindingContext().get(BindingContext.VARIABLE, declaration);
                        assert descriptor != null;
                        if(descriptor.getReceiverParameter().exists()) {
                            continue;
                        }
                        codegen.genToJVMStack(initializer);
                        codegen.intermediateValueForProperty(descriptor, true, null).store(new InstructionAdapter(mv));
                    }
                }
            }

            mv.visitInsn(RETURN);
            FunctionCodegen.endVisit(mv, "static initializer for namespace", namespace);
            mv.visitEnd();
        }
    }

    private static boolean hasNonConstantPropertyInitializers(JetFile namespace) {
        for (JetDeclaration declaration : namespace.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                if (initializer != null && !(initializer instanceof JetConstantExpression)) {
                    return true;
                }

            }
        }
        return false;
    }

    public void done() {
        v.done();
    }

    @NotNull
    public static JvmClassName getJVMClassNameForKotlinNs(@NotNull FqName fqName) {
        if (fqName.isRoot()) {
            return JvmClassName.byInternalName(JvmAbi.PACKAGE_CLASS);
        }

        String name = fqName.getFqName().replace('.', '/');
        if (name.startsWith(JavaDescriptorResolver.JAVA_ROOT)) {
            name = name.substring(JavaDescriptorResolver.JAVA_ROOT.length() + 1, name.length());
        }
        name += "/" + JvmAbi.PACKAGE_CLASS;
        return JvmClassName.byInternalName(name);
    }
}
