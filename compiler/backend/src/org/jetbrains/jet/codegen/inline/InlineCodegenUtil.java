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

package org.jetbrains.jet.codegen.inline;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.PackageContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils;
import org.jetbrains.jet.lang.resolve.kotlin.PackagePartClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFqName;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isTrait;

public class InlineCodegenUtil {
    public static final int API = Opcodes.ASM5;
    public static final String INVOKE = "invoke";
    public static final boolean DEFAULT_INLINE_FLAG = true;

    public static final String CAPTURED_FIELD_PREFIX = "$";

    public static final String THIS$0 = "this$0";

    public static final String RECEIVER$0 = "receiver$0";

    @Nullable
    public static MethodNode getMethodNode(
            InputStream classData,
            final String methodName,
            final String methodDescriptor
    ) throws ClassNotFoundException, IOException {
        ClassReader cr = new ClassReader(classData);
        final MethodNode[] methodNode = new MethodNode[1];
        cr.accept(new ClassVisitor(API) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (methodName.equals(name) && methodDescriptor.equals(desc)) {
                    return methodNode[0] = new MethodNode(access, name, desc, signature, exceptions);
                }
                return null;
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return methodNode[0];
    }


    @NotNull
    public static VirtualFile getVirtualFileForCallable(@NotNull DeserializedSimpleFunctionDescriptor deserializedDescriptor, @NotNull GenerationState state) {
        VirtualFile file;
        DeclarationDescriptor parentDeclaration = deserializedDescriptor.getContainingDeclaration();
        if (parentDeclaration instanceof PackageFragmentDescriptor) {
            ProtoBuf.Callable proto = deserializedDescriptor.getProto();
            if (!proto.hasExtension(JavaProtoBuf.implClassName)) {
                throw new IllegalStateException("Function in namespace should have implClassName property in proto: " + deserializedDescriptor);
            }
            Name name = deserializedDescriptor.getNameResolver().getName(proto.getExtension(JavaProtoBuf.implClassName));
            FqName packagePartFqName =
                    PackageClassUtils.getPackageClassFqName(((PackageFragmentDescriptor) parentDeclaration).getFqName()).parent().child(
                            name);
            file = findVirtualFileWithHeader(state.getProject(), packagePartFqName);
        } else {
            file = findVirtualFileContainingDescriptor(state.getProject(), deserializedDescriptor);
        }

        if (file == null) {
            throw new IllegalStateException("Couldn't find declaration file for " + deserializedDescriptor.getName());
        }

        return file;
    }

    @Nullable
    public static VirtualFile findVirtualFileWithHeader(@NotNull Project project, @NotNull FqName containerFqName) {
        VirtualFileFinder fileFinder = ServiceManager.getService(project, VirtualFileFinder.class);
        return fileFinder.findVirtualFileWithHeader(containerFqName);
    }

    @Nullable
    public static VirtualFile findVirtualFile(@NotNull Project project, @NotNull String internalName) {
        VirtualFileFinder fileFinder = ServiceManager.getService(project, VirtualFileFinder.class);
        return fileFinder.findVirtualFile(internalName);
    }

    //TODO: navigate to inner classes
    @Nullable
    public static FqName getContainerFqName(@NotNull DeclarationDescriptor referencedDescriptor) {
        ClassOrPackageFragmentDescriptor
                containerDescriptor = DescriptorUtils.getParentOfType(referencedDescriptor, ClassOrPackageFragmentDescriptor.class, false);
        if (containerDescriptor instanceof PackageFragmentDescriptor) {
            return PackageClassUtils.getPackageClassFqName(getFqName(containerDescriptor).toSafe());
        }
        if (containerDescriptor instanceof ClassDescriptor) {
            FqName fqName = DeserializedResolverUtils.kotlinFqNameToJavaFqName(getFqName(containerDescriptor));
            if (isTrait(containerDescriptor)) {
                return fqName.parent().child(Name.identifier(fqName.shortName() + JvmAbi.TRAIT_IMPL_SUFFIX));
            }
            return fqName;
        }
        return null;
    }

    public static String getInlineName(@NotNull CodegenContext codegenContext, @NotNull JetTypeMapper typeMapper) {
        return getInlineName(codegenContext, codegenContext.getContextDescriptor(), typeMapper);
    }

    private static String getInlineName(@NotNull CodegenContext codegenContext, @NotNull DeclarationDescriptor currentDescriptor, @NotNull JetTypeMapper typeMapper) {
        if (currentDescriptor instanceof PackageFragmentDescriptor) {
            PsiFile file = getContainingFile(codegenContext, typeMapper);

            Type packagePartType;
            if (file == null) {
                //in case package fragment clinit
                assert codegenContext instanceof PackageContext : "Expected package context but " + codegenContext;
                packagePartType = ((PackageContext) codegenContext).getPackagePartType();
            } else {
                packagePartType = PackagePartClassUtils.getPackagePartType((JetFile) file);
            }

            if (packagePartType == null) {
                DeclarationDescriptor contextDescriptor = codegenContext.getContextDescriptor();
                //noinspection ConstantConditions
                throw new RuntimeException("Couldn't find declaration for " + contextDescriptor.getContainingDeclaration().getName() + "." + contextDescriptor.getName() );
            }

            return packagePartType.getInternalName();
        }
        else if (currentDescriptor instanceof ClassifierDescriptor) {
            Type type = typeMapper.mapType((ClassifierDescriptor) currentDescriptor);
            return type.getInternalName();
        } else if (currentDescriptor instanceof FunctionDescriptor) {
            ClassDescriptor descriptor =
                    typeMapper.getBindingContext().get(CodegenBinding.CLASS_FOR_FUNCTION, (FunctionDescriptor) currentDescriptor);
            if (descriptor != null) {
                Type type = typeMapper.mapType(descriptor);
                return type.getInternalName();
            }
        }

        //TODO: add suffix for special case
        String suffix = currentDescriptor.getName().isSpecial() ? "" : currentDescriptor.getName().asString();

        //noinspection ConstantConditions
        return getInlineName(codegenContext, currentDescriptor.getContainingDeclaration(), typeMapper) + "$" + suffix;
    }

    @Nullable
    private static VirtualFile findVirtualFileContainingDescriptor(
            @NotNull Project project,
            @NotNull DeclarationDescriptor referencedDescriptor
    ) {
        FqName containerFqName = getContainerFqName(referencedDescriptor);
        if (containerFqName == null) {
            return null;
        }
        return findVirtualFileWithHeader(project, containerFqName);
    }


    public static boolean isInvokeOnLambda(String owner, String name) {
        if (!INVOKE.equals(name)) {
            return false;
        }

        for (String prefix : Arrays.asList("kotlin/Function", "kotlin/ExtensionFunction")) {
            if (owner.startsWith(prefix)) {
                String suffix = owner.substring(prefix.length());
                if (isInteger(suffix)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isLambdaConstructorCall(@NotNull String internalName, @NotNull String methodName) {
        return "<init>".equals(methodName) && isLambdaClass(internalName);
    }

    public static boolean isLambdaClass(String internalName) {
        String shortName = getLastNamePart(internalName);
        int index = shortName.lastIndexOf("$");

        if (index < 0) {
            return false;
        }

        String suffix = shortName.substring(index + 1);
        return isInteger(suffix);
    }

    @NotNull
    private static String getLastNamePart(@NotNull String internalName) {
        int index = internalName.lastIndexOf("/");
        return index < 0 ? internalName : internalName.substring(index + 1);
    }

    @Nullable
    public static PsiFile getContainingFile(CodegenContext codegenContext, JetTypeMapper typeMapper) {
        DeclarationDescriptor contextDescriptor = codegenContext.getContextDescriptor();
        PsiElement psiElement = BindingContextUtils.descriptorToDeclaration(typeMapper.getBindingContext(), contextDescriptor);
        if (psiElement != null) {
            return psiElement.getContainingFile();
        }
        return null;
    }

    @NotNull
    public static MaxCalcNode wrapWithMaxLocalCalc(@NotNull MethodNode methodNode) {
        return new MaxCalcNode(methodNode);
    }

    private static boolean isInteger(@NotNull String string) {
        if (string.isEmpty()) {
            return false;
        }

        for (int i = 0; i < string.length(); i++) {
             if (!Character.isDigit(string.charAt(i))) {
                 return false;
             }
        }

        return true;
    }

    public static boolean isCapturedFieldName(@NotNull String fieldName) {
        // TODO: improve this heuristic
        return (fieldName.startsWith(CAPTURED_FIELD_PREFIX) && !fieldName.equals(JvmAbi.KOTLIN_CLASS_FIELD_NAME)) ||
               THIS$0.equals(fieldName) ||
               RECEIVER$0.equals(fieldName);
    }
}
