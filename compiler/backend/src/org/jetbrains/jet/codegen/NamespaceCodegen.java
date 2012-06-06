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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.utils.Progress;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.ArrayList;
import java.util.Collection;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author max
 */
public class NamespaceCodegen {
    private final ClassBuilder v;
    @NotNull private final FqName name;
    private final GenerationState state;
    private final Collection<JetFile> files;
    private int nextMultiFile = 0;

    public NamespaceCodegen(ClassBuilder v, @NotNull FqName fqName, GenerationState state, Collection<JetFile> files) {
        this.v = v;
        name = fqName;
        this.state = state;
        this.files = files;

        PsiFile sourceFile = files.iterator().next().getContainingFile();
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

    public void generate(CompilationErrorHandler errorHandler, Progress progress) {
        ArrayList<Pair<JetFile, Collection<JetDeclaration>>> byFile = new ArrayList<Pair<JetFile, Collection<JetDeclaration>>>();

        for (JetFile file : files) {
            ArrayList<JetDeclaration> fileFunctions = new ArrayList<JetDeclaration>();
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetNamedFunction) {
                    fileFunctions.add(declaration);
                }
            }
            if(fileFunctions.size() > 0)
                byFile.add(new Pair<JetFile, Collection<JetDeclaration>>(file, fileFunctions));
        }

        boolean multiFile = byFile.size() > 1;

        for (JetFile file : files) {
            VirtualFile vFile = file.getVirtualFile();
            try {
                String path = vFile != null ? vFile.getPath() : "no_virtual_file/" + file.getName();
                if(progress != null) progress.log("For source: " + path);
                generate(file, multiFile);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                if(errorHandler != null) errorHandler.reportException(e, vFile == null ? "no file" : vFile.getUrl());
                DiagnosticUtils.throwIfRunningOnServer(e);
                if (ApplicationManager.getApplication().isInternal()) {
                    e.printStackTrace();
                }
            }
        }

        if (hasNonConstantPropertyInitializers()) {
            generateStaticInitializers();
        }
    }

    private void generate(JetFile file, boolean multiFile) {
        for (JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                NamespaceDescriptor descriptor = state.getBindingContext().get(BindingContext.FILE_TO_NAMESPACE, file);
                final CodegenContext context = CodegenContexts.STATIC.intoNamespace(descriptor);
                state.getInjector().getMemberCodegen().generateFunctionOrProperty(
                        (JetTypeParameterListOwner) declaration, context, v);
            }
            else if (declaration instanceof JetNamedFunction) {
                if(!multiFile) {
                    NamespaceDescriptor descriptor = state.getBindingContext().get(BindingContext.FILE_TO_NAMESPACE, file);
                    final CodegenContext context = CodegenContexts.STATIC.intoNamespace(descriptor);
                    state.getInjector().getMemberCodegen().generateFunctionOrProperty(
                            (JetTypeParameterListOwner) declaration, context, v);
                }
            }
            else if (declaration instanceof JetClassOrObject) {
                NamespaceDescriptor descriptor = state.getBindingContext().get(BindingContext.FILE_TO_NAMESPACE, file);
                final CodegenContext context = CodegenContexts.STATIC.intoNamespace(descriptor);
                state.getInjector().getClassCodegen().generate(context, (JetClassOrObject) declaration);
            }
            else if (declaration instanceof JetScript) {
                state.getInjector().getScriptCodegen().generate((JetScript) declaration);
            }
        }

        if(multiFile) {
            int k = 0;
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetNamedFunction) {
                    k++;
                }
            }

            if(k > 0) {
                PsiFile containingFile = file.getContainingFile();
                String className = name.getFqName().replace('.','/') + "namespace$src$" + (nextMultiFile++);
                ClassBuilder builder = state.forNamespacepart(className, file);

                builder.defineClass(containingFile, V1_6,
                              ACC_PUBLIC/*|ACC_SUPER*/,
                              className,
                              null,
                              //"jet/lang/Namespace",
                              "java/lang/Object",
                              new String[0]
                );
                builder.visitSource(containingFile.getName(), null);

                for (JetDeclaration declaration : file.getDeclarations()) {
                    if (declaration instanceof JetNamedFunction) {
                        NamespaceDescriptor descriptor = state.getBindingContext().get(BindingContext.FILE_TO_NAMESPACE, file);
                        {
                            final CodegenContext context = CodegenContexts.STATIC.intoNamespace(descriptor);
                            state.getInjector().getMemberCodegen().generateFunctionOrProperty((JetTypeParameterListOwner) declaration, context, builder);
                        }
                        {
                            final CodegenContext context = CodegenContexts.STATIC.intoNamespacePart(className, descriptor);
                            state.getInjector().getMemberCodegen().generateFunctionOrProperty((JetTypeParameterListOwner) declaration, context, v);
                        }
                    }
                }

                builder.done();
            }
        }
    }

    private void generateStaticInitializers() {
        JetFile namespace = files.iterator().next(); // @todo: hack

        MethodVisitor mv = v.newMethod(namespace, ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
        for (JetFile file : files) {
            if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
                mv.visitCode();

                FrameMap frameMap = new FrameMap();
                ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, CodegenContexts.STATIC, state);

                for (JetDeclaration declaration : file.getDeclarations()) {
                    if (declaration instanceof JetProperty) {
                        final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                        if (initializer != null && !(initializer instanceof JetConstantExpression)) {
                            final PropertyDescriptor descriptor = (PropertyDescriptor) state.getBindingContext().get(BindingContext.VARIABLE, declaration);
                            assert descriptor != null;
                            if(descriptor.getReceiverParameter().exists()) {
                                continue;
                            }
                            codegen.genToJVMStack(initializer);
                            StackValue.Property propValue = codegen.intermediateValueForProperty(descriptor, true, null);
                            propValue.store(propValue.type, new InstructionAdapter(mv));
                        }
                    }
                }
            }
        }

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitInsn(RETURN);
            FunctionCodegen.endVisit(mv, "static initializer for namespace", namespace);
            mv.visitEnd();
        }
    }

    private boolean hasNonConstantPropertyInitializers() {
        for (JetFile file : files) {
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetProperty) {
                    final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                    if (initializer != null && !(initializer instanceof JetConstantExpression)) {
                        return true;
                    }

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

        return JvmClassName.byInternalName(fqName.getFqName().replace('.', '/') + "/" + JvmAbi.PACKAGE_CLASS);
    }
}
