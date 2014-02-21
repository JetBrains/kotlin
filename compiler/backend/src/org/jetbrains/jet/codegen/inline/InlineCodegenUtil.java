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
import org.jetbrains.asm4.*;
import org.jetbrains.asm4.tree.MethodNode;
import org.jetbrains.jet.codegen.PackageCodegen;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.PackageContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.io.IOException;
import java.io.InputStream;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFqName;

public class InlineCodegenUtil {

    public final static int API = Opcodes.ASM4;

    public final static String INVOKE = "invoke";

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
    public static VirtualFile getVirtualFileForCallable(DeserializedSimpleFunctionDescriptor deserializedDescriptor, GenerationState state) {
        VirtualFile file = null;
        DeclarationDescriptor parentDeclatation = deserializedDescriptor.getContainingDeclaration();
        if (parentDeclatation instanceof PackageFragmentDescriptor) {
            ProtoBuf.Callable proto = deserializedDescriptor.getFunctionProto();
            if (proto.hasExtension(JavaProtoBuf.implClassName)) {
                Name name = deserializedDescriptor.getNameResolver().getName(proto.getExtension(JavaProtoBuf.implClassName));
                FqName namespaceFqName =
                        PackageClassUtils.getPackageClassFqName(((PackageFragmentDescriptor) parentDeclatation).getFqName()).parent().child(
                                name);
                file = findVirtualFile(state.getProject(), namespaceFqName, true);
            } else {
                assert false : "Function in namespace should have implClassName property in proto: " + deserializedDescriptor;
            }
        } else {
            file = findVirtualFileContainingDescriptor(state.getProject(), deserializedDescriptor);
        }

        if (file == null) {
            throw new RuntimeException("Couldn't find declaration file for " + deserializedDescriptor.getName());
        }

        return file;
    }

    @Nullable
    public static VirtualFile findVirtualFile(@NotNull Project project, @NotNull FqName containerFqName, boolean onlyKotlin) {
        VirtualFileFinder fileFinder = ServiceManager.getService(project, VirtualFileFinder.class);
        if (onlyKotlin) {
            return fileFinder.findVirtualFile(containerFqName);
        } else {
            return fileFinder.findVirtualFile(containerFqName.asString().replace('.', '/'));
        }
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
            ClassKind classKind = ((ClassDescriptor) containerDescriptor).getKind();
            if (classKind == ClassKind.CLASS_OBJECT || classKind == ClassKind.ENUM_ENTRY) {
                return getContainerFqName(containerDescriptor.getContainingDeclaration());
            }
            return getFqName(containerDescriptor).toSafe();
        }
        return null;
    }

    public static String getInlineName(@NotNull CodegenContext codegenContext, @NotNull JetTypeMapper typeMapper) {
        return getInlineName(codegenContext, codegenContext.getContextDescriptor(), typeMapper);
    }

    private static String getInlineName(@NotNull CodegenContext codegenContext, @NotNull DeclarationDescriptor currentDescriptor, @NotNull JetTypeMapper typeMapper) {
        PsiFile file;
        if (currentDescriptor instanceof PackageFragmentDescriptor) {
            file = getContainingFile(codegenContext, typeMapper);

            Type packagePartType;
            if (file == null) {
                //in case package fragment clinit
                assert codegenContext instanceof PackageContext : "Expected package context but " + codegenContext;
                packagePartType = ((PackageContext) codegenContext).getPackagePartType();
            } else {
                packagePartType =
                        PackageCodegen.getPackagePartType(PackageClassUtils.getPackageClassFqName(getFqName(currentDescriptor).toSafe()),
                                                          file.getVirtualFile());
            }

            if (packagePartType == null) {
                DeclarationDescriptor contextDescriptor = codegenContext.getContextDescriptor();
                //noinspection ConstantConditions
                throw new RuntimeException("Couldn't find declaration for " + contextDescriptor.getContainingDeclaration().getName() + "." + contextDescriptor.getName() );
            }

            return packagePartType.getInternalName().replace('.', '/');
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
        return findVirtualFile(project, containerFqName, true);
    }


    public static boolean isInvokeOnInlinable(String owner, String name) {
        return INVOKE.equals(name) && /*TODO: check type*/owner.contains("Function");
    }

    public static boolean isLambdaConstructorCall(@NotNull String internalName, @NotNull String name) {
        if (!"<init>".equals(name)) {
            return false;
        }

        return isLambdaClass(internalName);
    }

    public static boolean isLambdaClass(String internalName) {
        String shortName = getLastNamePart(internalName);
        int index = shortName.lastIndexOf("$");

        if (index < 0) {
            return false;
        }

        String suffix = shortName.substring(index + 1);
        for (char c : suffix.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    @NotNull
    private static String getLastNamePart(@NotNull String internalName) {
        int index = internalName.lastIndexOf("/");
        return index < 0 ? internalName : internalName.substring(index + 1);
    }

    public static boolean isInitCallOfFunction(String owner, String name) {
        return "<init>".equals(name);
    }

    @Nullable
    public static PsiFile getContainingFile(CodegenContext codegenContext, JetTypeMapper typeMapper) {
        DeclarationDescriptor contextDescriptor = codegenContext.getContextDescriptor();
        PsiElement psiElement = BindingContextUtils.descriptorToDeclaration(typeMapper.getBindingContext(), contextDescriptor);
        if (psiElement == null) {
            //in case of synthetic
            psiElement = BindingContextUtils.descriptorToDeclaration(typeMapper.getBindingContext(), contextDescriptor);
        }

        if (psiElement != null) {
            return psiElement.getContainingFile();
        }
        return null;
    }

    @NotNull
    public static MaxCalcNode wrapWithMaxLocalCalc(@NotNull MethodNode methodNode) {
        return new MaxCalcNode(methodNode);
    }
}
