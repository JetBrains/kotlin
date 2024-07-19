/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copy-pasted and updated from com.intellij.psi.impl.source.ClassInnerStuffCache
 *
 * @see com.intellij.psi.impl.source.ClassInnerStuffCache
 */
public final class ClassInnerStuffCache {
    public static final String NOT_NULL_ANNOTATION_QUALIFIER = "@" + NotNull.class.getName();

    private final @NotNull KtExtensibleLightClass myClass;
    private final @NotNull List<ModificationTracker> myModificationTrackers;

    public ClassInnerStuffCache(
            @NotNull KtExtensibleLightClass aClass,
            @NotNull List<ModificationTracker> modificationTrackers
    ) {
        myClass = aClass;
        myModificationTrackers = modificationTrackers;
    }

    @Nullable
    public PsiField findFieldByName(String name, boolean checkBases) {
        if (checkBases) {
            return PsiClassImplUtil.findFieldByName(myClass, name, true);
        }
        else {
            return CachedValuesManager.getCachedValue(
                    myClass,
                    () -> CachedValueProvider.Result.create(
                            getFieldsMap(),
                            myModificationTrackers
                    )
            ).get(name);
        }
    }

    @NotNull
    public PsiMethod[] findMethodsByName(String name, boolean checkBases) {
        if (checkBases) {
            return PsiClassImplUtil.findMethodsByName(myClass, name, true);
        }
        else {
            List<PsiMethod> methods = CachedValuesManager.getCachedValue(
                    myClass,
                    () -> CachedValueProvider.Result.create(
                            getMethodsMap(),
                            myModificationTrackers
                    )
            ).get(name);

            return methods == null || methods.isEmpty() ? PsiMethod.EMPTY_ARRAY : methods.toArray(PsiMethod.EMPTY_ARRAY);
        }
    }

    @Nullable
    public PsiClass findInnerClassByName(String name, boolean checkBases) {
        if (checkBases) {
            return PsiClassImplUtil.findInnerByName(myClass, name, true);
        }
        else {
            return CachedValuesManager.getCachedValue(
                    myClass,
                    () -> CachedValueProvider.Result.create(
                            getInnerClassesMap(),
                            myModificationTrackers
                    )
            ).get(name);
        }
    }

    @NotNull
    private Map<String, PsiField> getFieldsMap() {
        Map<String, PsiField> cachedFields = new HashMap<>();
        for (PsiField field : myClass.getOwnFields()) {
            String name = field.getName();
            cachedFields.putIfAbsent(name, field);
        }

        return cachedFields;
    }

    @NotNull
    private Map<String, List<PsiMethod>> getMethodsMap() {
        return myClass.getOwnMethods().stream().collect(Collectors.groupingBy(PsiMethod::getName));
    }

    @NotNull
    private Map<String, PsiClass> getInnerClassesMap() {
        Map<String, PsiClass> cachedInners = new HashMap<>();
        for (PsiClass psiClass : myClass.getOwnInnerClasses()) {
            String name = psiClass.getName();
            if (name == null) {
                Logger.getInstance(ClassInnerStuffCache.class).error("$psiClass has no name");
            }
            else {
                cachedInners.putIfAbsent(name, psiClass);
            }
        }

        return cachedInners;
    }
}