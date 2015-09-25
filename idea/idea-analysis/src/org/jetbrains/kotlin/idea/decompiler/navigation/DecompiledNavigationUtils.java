/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler.navigation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.idea.decompiler.KotlinClsFileBase;
import org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope;
import org.jetbrains.kotlin.idea.stubindex.StaticFacadeIndexUtil;
import org.jetbrains.kotlin.idea.vfilefinder.JsVirtualFileFinderFactory;
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.serialization.deserialization.NameResolver;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptPackageFragment;
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;

import java.util.Collection;

import static org.jetbrains.kotlin.load.kotlin.PackageClassUtils.getPackageClassId;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getClassId;

public final class DecompiledNavigationUtils {

    @Nullable
    public static JetDeclaration getDeclarationFromDecompiledClassFile(
            @NotNull Project project,
            @NotNull DeclarationDescriptor referencedDescriptor
    ) {
        if (isLocal(referencedDescriptor)) return null;

        VirtualFile virtualFile = findVirtualFileContainingDescriptor(project, referencedDescriptor);

        if (virtualFile == null) return null;

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (!(psiFile instanceof KotlinClsFileBase)) {
            return null;
        }

        return ((KotlinClsFileBase) psiFile).getDeclarationForDescriptor(referencedDescriptor);
    }

    private static boolean isLocal(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ParameterDescriptor) {
            return isLocal(descriptor.getContainingDeclaration());
        }
        else {
            return DescriptorUtils.isLocal(descriptor);
        }
    }

    /*
        Find virtual file which contains the declaration of descriptor we're navigating to.
     */
    @Nullable
    private static VirtualFile findVirtualFileContainingDescriptor(
            @NotNull Project project,
            @NotNull DeclarationDescriptor referencedDescriptor
    ) {
        if (ErrorUtils.isError(referencedDescriptor)) return null;

        ClassId containerClassId = getContainerClassId(project, referencedDescriptor);
        if (containerClassId == null) return null;

        GlobalSearchScope scopeToSearchIn = JetSourceFilterScope.kotlinSourceAndClassFiles(GlobalSearchScope.allScope(project), project);

        VirtualFileFinderFactory virtualFileFinderFactory;
        if (isFromKotlinJavasriptMetadata(referencedDescriptor)) {
            virtualFileFinderFactory = JsVirtualFileFinderFactory.SERVICE.getInstance(project);
        }
        else {
            virtualFileFinderFactory = JvmVirtualFileFinderFactory.SERVICE.getInstance(project);
        }

        VirtualFileFinder fileFinder = virtualFileFinderFactory.create(scopeToSearchIn);
        return fileFinder.findVirtualFileWithHeader(containerClassId);
    }

    private static boolean isFromKotlinJavasriptMetadata(@NotNull DeclarationDescriptor referencedDescriptor) {
        PackageFragmentDescriptor packageFragmentDescriptor =
                DescriptorUtils.getParentOfType(referencedDescriptor, PackageFragmentDescriptor.class, false);
        return packageFragmentDescriptor instanceof KotlinJavascriptPackageFragment;
    }

    //TODO: navigate to inner classes
    //TODO: should we construct proper SourceElement's for decompiled parts / facades?
    @Nullable
    private static ClassId getContainerClassId(@NotNull Project project, @NotNull DeclarationDescriptor referencedDescriptor) {
        DeserializedCallableMemberDescriptor deserializedCallableContainer =
                DescriptorUtils.getParentOfType(referencedDescriptor, DeserializedCallableMemberDescriptor.class, true);
        if (deserializedCallableContainer != null) {
            return getContainerClassId(project, deserializedCallableContainer);
        }

        ClassOrPackageFragmentDescriptor
                containerDescriptor = DescriptorUtils.getParentOfType(referencedDescriptor, ClassOrPackageFragmentDescriptor.class, false);
        if (containerDescriptor instanceof PackageFragmentDescriptor) {
            FqName packageFQN = ((PackageFragmentDescriptor) containerDescriptor).getFqName();

            if (referencedDescriptor instanceof DeserializedCallableMemberDescriptor) {
                DeserializedCallableMemberDescriptor deserializedDescriptor = (DeserializedCallableMemberDescriptor) referencedDescriptor;
                ProtoBuf.Callable proto = deserializedDescriptor.getProto();
                NameResolver nameResolver = deserializedDescriptor.getNameResolver();
                if (proto.hasExtension(JvmProtoBuf.implClassName)) {
                    Name partClassName = nameResolver.getName(proto.getExtension(JvmProtoBuf.implClassName));
                    FqName partFQN = packageFQN.child(partClassName);
                    Collection<JetFile> multifileFacadeJetFiles =
                            StaticFacadeIndexUtil.getMultifileClassForPart(partFQN, GlobalSearchScope.allScope(project), project);
                    if (multifileFacadeJetFiles.isEmpty()) {
                        return new ClassId(packageFQN, partClassName);
                    }
                    else {
                        JetFile multifileFacade = multifileFacadeJetFiles.iterator().next();
                        String multifileFacadeName = multifileFacade.getVirtualFile().getNameWithoutExtension();
                        return new ClassId(packageFQN, Name.identifier(multifileFacadeName));
                    }
                }
            }

            return getPackageClassId(packageFQN);
        }
        if (containerDescriptor instanceof ClassDescriptor) {
            if (containerDescriptor.getContainingDeclaration() instanceof ClassDescriptor
                || ExpressionTypingUtils.isLocal(containerDescriptor.getContainingDeclaration(), containerDescriptor)) {
                return getContainerClassId(project, containerDescriptor.getContainingDeclaration());
            }
            return getClassId((ClassDescriptor) containerDescriptor);
        }
        return null;
    }

    private DecompiledNavigationUtils() {
    }
}
