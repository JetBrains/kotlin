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

package org.jetbrains.jet.plugin.caches;

import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.search.GlobalSearchScope;
import jet.KotlinClass;
import jet.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.resolve.java.JavaResolverPsiUtils;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinClassFinder;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader;
import org.jetbrains.jet.lang.resolve.kotlin.header.SerializedDataHeader;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Number of helper methods for searching jet element prototypes in java. Methods use java indices for search.
 */
public class JetFromJavaDescriptorHelper {

    private JetFromJavaDescriptorHelper() {
    }

    /**
     * Get java equivalents for jet top level classes.
     */
    static Collection<PsiClass> getClassesForJetNamespaces(Project project, GlobalSearchScope scope) {
        /* Will iterate through short name caches
           Kotlin namespaces from jar a class files will be collected from java cache
           Kotlin namespaces classes from sources will be collected with JetShortNamesCache.getClassesByName */
        return getClassesByAnnotation(KotlinPackage.class.getSimpleName(), project, scope);
    }

    /**
     * Get names that could have jet descriptor equivalents. It could be inaccurate and return more results than necessary.
     */
    static Collection<String> getPossiblePackageDeclarationsNames(Project project, GlobalSearchScope scope) {
        Collection<String> result = new ArrayList<String>();

        for (PsiClass jetNamespaceClass : getClassesForJetNamespaces(project, scope)) {
            for (PsiMethod psiMethod : jetNamespaceClass.getMethods()) {
                if (psiMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                    result.add(psiMethod.getName());
                }
            }
        }

        return result;
    }

    static Collection<PsiClass> getCompiledClassesForTopLevelObjects(Project project, GlobalSearchScope scope) {
        Set<PsiClass> jetObjectClasses = Sets.newHashSet();

        Collection<PsiClass> classesByAnnotation = getClassesByAnnotation(KotlinClass.class.getSimpleName(), project, scope);

        for (PsiClass psiClass : classesByAnnotation) {
            ClassKind kind = getCompiledClassKind(psiClass);
            if (kind == null) {
                continue;
            }
            if (psiClass.getContainingClass() == null && kind == ClassKind.OBJECT) {
                jetObjectClasses.add(psiClass);
            }
        }

        return jetObjectClasses;
    }

    @Nullable
    public static ClassKind getCompiledClassKind(@NotNull PsiClass psiClass) {
        ClassData classData = getClassData(psiClass);
        if (classData == null) return null;
        return DescriptorDeserializer.classKind(Flags.CLASS_KIND.get(classData.getClassProto().getFlags()));
    }


    @Nullable
    private static ClassData getClassData(@NotNull PsiClass psiClass) {
        String[] data = getAnnotationDataForKotlinClass(psiClass);
        return data == null ? null : JavaProtoBufUtil.readClassDataFrom(data);
    }

    @Nullable
    private static PackageData getPackageData(@NotNull PsiClass psiClass) {
        String[] data = getAnnotationDataForKotlinClass(psiClass);
        return data == null ? null : JavaProtoBufUtil.readPackageDataFrom(data);
    }

    @Nullable
    private static String[] getAnnotationDataForKotlinClass(@NotNull PsiClass psiClass) {
        VirtualFile virtualFile = getVirtualFileForPsiClass(psiClass);
        if (virtualFile != null) {
            KotlinJvmBinaryClass kotlinClass = KotlinClassFinder.SERVICE.getInstance(psiClass.getProject()).createKotlinClass(virtualFile);
            KotlinClassHeader header = KotlinClassHeader.read(kotlinClass);
            if (header instanceof SerializedDataHeader) {
                return ((SerializedDataHeader) header).getAnnotationData();
            }
        }
        return null;
    }

    //TODO: common utility
    //TODO: doesn't work for inner classes and stuff
    @Nullable
    private static VirtualFile getVirtualFileForPsiClass(@NotNull PsiClass psiClass) {
        PsiFile psiFile = psiClass.getContainingFile();
        return psiFile == null ? null : psiFile.getVirtualFile();
    }

    @Nullable
    static FqName getJetTopLevelDeclarationFQN(@NotNull PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();

        if (containingClass != null) {
            String qualifiedName = containingClass.getQualifiedName();
            assert qualifiedName != null;

            FqName classFQN = new FqName(qualifiedName);

            if (JavaResolverPsiUtils.isCompiledKotlinPackageClass(containingClass)) {
                FqName classParentFQN = QualifiedNamesUtil.withoutLastSegment(classFQN);
                return QualifiedNamesUtil.combine(classParentFQN, Name.identifier(method.getName()));
            }
        }

        return null;
    }

    private static Collection<PsiClass> getClassesByAnnotation(
            String annotationName, Project project, GlobalSearchScope scope
    ) {
        Collection<PsiClass> classes = Sets.newHashSet();
        Collection<PsiAnnotation> annotations = JavaAnnotationIndex.getInstance().get(annotationName, project, scope);
        for (PsiAnnotation annotation : annotations) {
            PsiModifierList modifierList = (PsiModifierList) annotation.getParent();
            PsiElement owner = modifierList.getParent();
            if (owner instanceof PsiClass) {
                classes.add((PsiClass) owner);
            }
        }
        return classes;
    }


    @NotNull
    public static Collection<FqName> getTopLevelFunctionFqNames(
            @NotNull Project project,
            @NotNull GlobalSearchScope scope,
            boolean shouldBeExtension
    ) {
        Collection<FqName> result = Sets.newHashSet();
        Collection<PsiClass> packageClasses = getClassesByAnnotation(KotlinPackage.class.getSimpleName(), project, scope);
        for (PsiClass psiClass : packageClasses) {
            String qualifiedName = psiClass.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }
            FqName packageFqName = new FqName(qualifiedName).parent();
            PackageData data = getPackageData(psiClass);
            if (data == null) {
                continue;
            }
            NameResolver nameResolver = data.getNameResolver();
            for (ProtoBuf.Callable callable : data.getPackageProto().getMemberList()) {
                if (callable.hasReceiverType() == shouldBeExtension) {
                    Name name = nameResolver.getName(callable.getName());
                    result.add(packageFqName.child(name));
                }
            }
        }
        return result;
    }
}
