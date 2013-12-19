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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolverUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.MemberComparator;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader;
import org.jetbrains.jet.lang.resolve.kotlin.header.SerializedDataHeader;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isEnumEntry;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isSyntheticClassObject;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN_SOURCES;
import static org.jetbrains.jet.plugin.libraries.JetDecompiledData.descriptorToKey;

public final class DecompiledDataFactory {
    private static final String DECOMPILED_COMMENT = "/* compiled code */";
    public static final DescriptorRenderer DESCRIPTOR_RENDERER =
            new DescriptorRendererBuilder().setWithDefinedIn(false).setClassWithPrimaryConstructor(true).build();
    @NotNull
    private final StringBuilder builder = new StringBuilder();
    @NotNull
    private final Map<String, TextRange> renderedDescriptorsToRange = new HashMap<String, TextRange>();
    @NotNull
    private final JavaDescriptorResolver javaDescriptorResolver;
    @NotNull
    private final SerializedDataHeader classFileHeader;
    @NotNull
    private final FqName classFqName;
    @NotNull
    private final VirtualFile classFile;
    @NotNull
    private final Project project;

    private DecompiledDataFactory(@NotNull VirtualFile classFile, @NotNull Project project) {
        this.classFile = classFile;
        this.project = project;
        InjectorForJavaDescriptorResolver injector = InjectorForJavaDescriptorResolverUtil.create(project, new BindingTraceContext());
        this.javaDescriptorResolver = injector.getJavaDescriptorResolver();

        VirtualFileKotlinClass kotlinClass = new VirtualFileKotlinClass(classFile);
        this.classFqName = kotlinClass.getClassName().getFqNameForClassNameWithoutDollars();

        KotlinClassHeader header = KotlinClassHeader.read(kotlinClass);
        assert header instanceof SerializedDataHeader : "Decompiled data factory shouldn't be called on an unsupported file: " + classFile;
        this.classFileHeader = (SerializedDataHeader) header;
    }

    @NotNull
    static JetDecompiledData createDecompiledData(@NotNull VirtualFile virtualFile, @NotNull Project project) {
        return new DecompiledDataFactory(virtualFile, project).build();
    }

    private JetDecompiledData build() {
        FqName packageFqName = classFqName.parent();
        appendDecompiledTextAndPackageName(packageFqName);
        SerializedDataHeader.Kind kind = classFileHeader.getKind();
        if (kind == SerializedDataHeader.Kind.PACKAGE) {
            PackageFragmentDescriptor pf = javaDescriptorResolver.getPackageFragmentProvider().getOrCreatePackage(packageFqName);
            if (pf != null) {
                for (DeclarationDescriptor member : sortDeclarations(pf.getMemberScope().getAllDescriptors())) {
                    if (!(member instanceof ClassDescriptor)) {
                        appendDescriptor(member, "");
                        builder.append("\n");
                    }
                }
            }
        }
        else if (kind == SerializedDataHeader.Kind.CLASS) {
            ClassDescriptor cd = javaDescriptorResolver.resolveClass(classFqName, INCLUDE_KOTLIN_SOURCES);
            if (cd != null) {
                appendDescriptor(cd, "");
            }
        }
        else {
            throw new UnsupportedOperationException("Unknown header kind: " + kind);
        }

        JetFile jetFile = JetDummyClassFileViewProvider.createJetFile(PsiManager.getInstance(project), classFile, builder.toString());
        return new JetDecompiledData(jetFile, renderedDescriptorsToRange);
    }

    private void appendDecompiledTextAndPackageName(@NotNull FqName packageName) {
        builder.append("// IntelliJ API Decompiler stub source generated from a class file\n" +
                       "// Implementation of methods is not available");
        builder.append("\n\n");
        if (!packageName.isRoot()) {
            builder.append("package ").append(packageName).append("\n\n");
        }
    }

    private static List<DeclarationDescriptor> sortDeclarations(@NotNull Collection<DeclarationDescriptor> input) {
        ArrayList<DeclarationDescriptor> r = new ArrayList<DeclarationDescriptor>(input);
        Collections.sort(r, MemberComparator.INSTANCE);
        return r;
    }

    private void appendDescriptor(@NotNull DeclarationDescriptor descriptor, String indent) {
        int startOffset = builder.length();
        String header = isEnumEntry(descriptor)
                        ? descriptor.getName().asString()
                        : DESCRIPTOR_RENDERER.render(descriptor).replace("= ...", "= " + DECOMPILED_COMMENT);
        builder.append(header);
        int endOffset = builder.length();

        if (descriptor instanceof FunctionDescriptor || descriptor instanceof PropertyDescriptor) {
            if (((CallableMemberDescriptor) descriptor).getModality() != Modality.ABSTRACT) {
                if (descriptor instanceof FunctionDescriptor) {
                    builder.append(" { ").append(DECOMPILED_COMMENT).append(" }");
                    endOffset = builder.length();
                }
                else { // descriptor instanceof PropertyDescriptor
                    builder.append(" ").append(DECOMPILED_COMMENT);
                }
            }
        }
        else if (descriptor instanceof ClassDescriptor && !isEnumEntry(descriptor)) {
            builder.append(" {\n");
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            boolean firstPassed = false;
            String subindent = indent + "    ";
            ClassDescriptor classObject = classDescriptor.getClassObjectDescriptor();
            if (classObject != null && !isSyntheticClassObject(classObject)) {
                firstPassed = true;
                builder.append(subindent);
                appendDescriptor(classObject, subindent);
            }
            for (DeclarationDescriptor member : sortDeclarations(classDescriptor.getDefaultType().getMemberScope().getAllDescriptors())) {
                if (member.getContainingDeclaration() != descriptor) {
                    continue;
                }
                if (member instanceof CallableMemberDescriptor && ((CallableMemberDescriptor) member).getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
                    continue;
                }

                if (firstPassed) {
                    builder.append("\n");
                }
                else {
                    firstPassed = true;
                }
                builder.append(subindent);
                appendDescriptor(member, subindent);
            }
            builder.append(indent).append("}");
            endOffset = builder.length();
        }

        builder.append("\n");
        saveDescriptorToRange(descriptor, startOffset, endOffset);

        if (descriptor instanceof ClassDescriptor) {
            ConstructorDescriptor primaryConstructor = ((ClassDescriptor) descriptor).getUnsubstitutedPrimaryConstructor();
            if (primaryConstructor != null) {
                saveDescriptorToRange(primaryConstructor, startOffset, endOffset);
            }
        }
    }

    private void saveDescriptorToRange(DeclarationDescriptor descriptor, int startOffset, int endOffset) {
        renderedDescriptorsToRange.put(descriptorToKey(descriptor), new TextRange(startOffset, endOffset));
    }
}
