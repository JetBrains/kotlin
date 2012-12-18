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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.codegen.state.Progress;

import java.io.File;
import java.util.Collection;

import static org.jetbrains.asm4.Opcodes.*;

public class NamespaceCodegen extends MemberCodegen {
    @NotNull
    private final ClassBuilderOnDemand v;
    @NotNull private final FqName name;
    private final Collection<JetFile> files;

    public NamespaceCodegen(
            @NotNull ClassBuilderOnDemand v,
            @NotNull final FqName fqName,
            GenerationState state,
            Collection<JetFile> namespaceFiles
    ) {
        super(state);
        checkAllFilesHaveSameNamespace(namespaceFiles);

        this.v = v;
        name = fqName;
        this.files = namespaceFiles;

        final PsiFile sourceFile = namespaceFiles.iterator().next().getContainingFile();

        v.addOptionalDeclaration(new ClassBuilderOnDemand.ClassBuilderCallback() {
            @Override
            public void doSomething(@NotNull ClassBuilder v) {
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
        });
    }

    public void generate(CompilationErrorHandler errorHandler, final Progress progress) {
        boolean multiFile = CodegenBinding.isMultiFileNamespace(state.getBindingContext(), name);

        for (JetFile file : files) {
            VirtualFile vFile = file.getVirtualFile();
            try {
                generate(file, multiFile);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                if (errorHandler != null) errorHandler.reportException(e, vFile == null ? "no file" : vFile.getUrl());
                DiagnosticUtils.throwIfRunningOnServer(e);
                if (ApplicationManager.getApplication().isInternal()) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
            }
        }

        assert v.isActivated() == shouldGenerateNSClass(files) : "Different algorithms for generating namespace class and for heuristics";

        if (hasNonConstantPropertyInitializers()) {
            generateStaticInitializers();
        }
    }

    private void generate(JetFile file, boolean multiFile) {
        NamespaceDescriptor descriptor = state.getBindingContext().get(BindingContext.FILE_TO_NAMESPACE, file);
        for (JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetProperty) {
                final CodegenContext context = CodegenContext.STATIC.intoNamespace(descriptor);
                genFunctionOrProperty(context, (JetTypeParameterListOwner) declaration, v.getClassBuilder());
            }
            else if (declaration instanceof JetNamedFunction) {
                if (!multiFile) {
                    final CodegenContext context = CodegenContext.STATIC.intoNamespace(descriptor);
                    genFunctionOrProperty(context, (JetTypeParameterListOwner) declaration, v.getClassBuilder());
                }
            }
            else if (declaration instanceof JetClassOrObject) {
                final CodegenContext context = CodegenContext.STATIC.intoNamespace(descriptor);
                genClassOrObject(context, (JetClassOrObject) declaration);
            }
            else if (declaration instanceof JetScript) {
                state.getScriptCodegen().generate((JetScript) declaration);
            }
        }

        if (multiFile) {
            int k = 0;
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetNamedFunction) {
                    k++;
                }
            }

            if (k > 0) {
                String namespaceInternalName = JvmClassName.byFqNameWithoutInnerClasses(name.child(Name.identifier(JvmAbi.PACKAGE_CLASS))).getInternalName();
                String className = getMultiFileNamespaceInternalName(namespaceInternalName, file);
                ClassBuilder builder = state.getFactory().forNamespacepart(className, file);

                builder.defineClass(file, V1_6,
                                    ACC_PUBLIC/*|ACC_SUPER*/,
                                    className,
                                    null,
                                    //"jet/lang/Namespace",
                                    "java/lang/Object",
                                    new String[0]
                );
                builder.visitSource(file.getName(), null);

                for (JetDeclaration declaration : file.getDeclarations()) {
                    if (declaration instanceof JetNamedFunction) {
                        {
                            final CodegenContext context =
                                    CodegenContext.STATIC.intoNamespace(descriptor);
                            genFunctionOrProperty(context, (JetTypeParameterListOwner) declaration, builder);
                        }
                        {
                            final CodegenContext context =
                                    CodegenContext.STATIC.intoNamespacePart(className, descriptor);
                            genFunctionOrProperty(context, (JetTypeParameterListOwner) declaration, v.getClassBuilder());
                        }
                    }
                }

                builder.done();
            }
        }
    }

    /**
     * @param namespaceFiles all files should have same package name
     * @return
     */
    public static boolean shouldGenerateNSClass(Collection<JetFile> namespaceFiles) {
        checkAllFilesHaveSameNamespace(namespaceFiles);

        for (JetFile file : namespaceFiles) {
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetProperty || declaration instanceof JetNamedFunction) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void checkAllFilesHaveSameNamespace(Collection<JetFile> namespaceFiles) {
        FqName commonFqName = null;
        for (JetFile file : namespaceFiles) {
            FqName fqName = JetPsiUtil.getFQName(file);
            if (commonFqName != null) {
                if (!commonFqName.equals(fqName)) {
                    throw new IllegalArgumentException("All files should have same package name");
                }
            }
            else {
                commonFqName = JetPsiUtil.getFQName(file);
            }
        }
    }

    private void generateStaticInitializers() {
        final JetFile namespace = files.iterator().next(); // @todo: hack

        v.addOptionalDeclaration(new ClassBuilderOnDemand.ClassBuilderCallback() {
            @Override
            public void doSomething(@NotNull ClassBuilder v) {
                MethodVisitor mv = v.newMethod(namespace, ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
                if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
                    mv.visitCode();

                    FrameMap frameMap = new FrameMap();
                    ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, CodegenContext.STATIC, state);

                    for (JetFile file : files) {
                        for (JetDeclaration declaration : file.getDeclarations()) {
                            if (declaration instanceof JetProperty) {
                                final JetExpression initializer = ((JetProperty) declaration).getInitializer();
                                if (initializer != null && !(initializer instanceof JetConstantExpression)) {
                                    final PropertyDescriptor descriptor =
                                            (PropertyDescriptor) state.getBindingContext().get(BindingContext.VARIABLE, declaration);
                                    assert descriptor != null;
                                    codegen.genToJVMStack(initializer);
                                    StackValue.Property propValue = codegen.intermediateValueForProperty(descriptor, true, null);
                                    propValue.store(propValue.type, new InstructionAdapter(mv));
                                }
                            }
                        }
                    }

                    mv.visitInsn(RETURN);
                    FunctionCodegen.endVisit(mv, "static initializer for namespace", namespace);
                    mv.visitEnd();
                }
            }
        });
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

        return JvmClassName.byFqNameWithoutInnerClasses(fqName.child(Name.identifier(JvmAbi.PACKAGE_CLASS)));
    }

    @NotNull
    private static String getMultiFileNamespaceInternalName(@NotNull String namespaceInternalName, @NotNull PsiFile file) {
        String name = FileUtil.toSystemDependentName(file.getName());

        int substringFrom = name.lastIndexOf(File.separator) + 1;

        int substringTo = name.lastIndexOf('.');
        if (substringTo == -1) {
            substringTo = name.length();
        }

        int pathHashCode = FileUtil.toSystemDependentName(file.getVirtualFile().getCanonicalPath()).hashCode();

        // dollar sign in the end is to prevent synthetic class from having "Test" or other parseable suffix
        // path hashCode to prevent same name / different path collision
        return namespaceInternalName + "$src$" + name.substring(substringFrom, substringTo) + "$" + pathHashCode;
    }

    @NotNull
    public static String getNamespacePartInternalName(@NotNull JetFile file) {
        FqName fqName = JetPsiUtil.getFQName(file);
        JvmClassName namespaceJvmClassName = NamespaceCodegen.getJVMClassNameForKotlinNs(fqName);
        String namespaceInternalName = namespaceJvmClassName.getInternalName();
        return NamespaceCodegen.getMultiFileNamespaceInternalName(namespaceInternalName, file);
    }
}