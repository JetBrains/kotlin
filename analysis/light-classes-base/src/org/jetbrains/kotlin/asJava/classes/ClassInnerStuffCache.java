/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Interner;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.util.ObjectUtils.notNull;

/**
 * Copy-pasted and updated from com.intellij.psi.impl.source.ClassInnerStuffCache
 *
 * @see com.intellij.psi.impl.source.ClassInnerStuffCache
 */
public final class ClassInnerStuffCache {
    public static final String NOT_NULL_ANNOTATION_QUALIFIER = "@" + NotNull.class.getName();

    private final @NotNull KtExtensibleLightClass myClass;
    private final @NotNull List<ModificationTracker> myModificationTrackers;
    private final @NotNull Ref<Pair<Long, Interner<PsiMember>>> myInterner = Ref.create();

    public ClassInnerStuffCache(
            @NotNull KtExtensibleLightClass aClass,
            @NotNull List<ModificationTracker> modificationTrackers
    ) {
        myClass = aClass;
        myModificationTrackers = modificationTrackers;
    }

    @NotNull
    public PsiMethod[] getConstructors() {
        return copy(CachedValuesManager.getCachedValue(
                myClass,
                () -> CachedValueProvider.Result.create(
                        PsiImplUtil.getConstructors(myClass),
                        myModificationTrackers
                )
        ));
    }

    @NotNull
    public PsiField[] getFields() {
        return copy(CachedValuesManager.getCachedValue(
                myClass,
                () -> CachedValueProvider.Result.create(
                        calcFields(),
                        myModificationTrackers
                )
        ));
    }

    @NotNull
    public PsiMethod[] getMethods() {
        return copy(CachedValuesManager.getCachedValue(
                myClass,
                () -> CachedValueProvider.Result.create(
                        calcMethods(),
                        myModificationTrackers
                )
        ));
    }

    @NotNull
    public PsiClass[] getInnerClasses() {
        return copy(CachedValuesManager.getCachedValue(
                myClass,
                () -> CachedValueProvider.Result.create(
                        calcInnerClasses(),
                        myModificationTrackers
                )
        ));
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
            return copy(notNull(CachedValuesManager.getCachedValue(
                    myClass,
                    () -> CachedValueProvider.Result.create(
                            getMethodsMap(),
                            myModificationTrackers
                    )
            ).get(name), PsiMethod.EMPTY_ARRAY));
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

    private static <T> T[] copy(T[] value) {
        return value.length == 0 ? value : value.clone();
    }

    @NotNull
    private PsiField[] calcFields() {
        List<PsiField> own = myClass.getOwnFields();
        List<PsiField> ext = internMembers(PsiAugmentProvider.collectAugments(myClass, PsiField.class, null));
        return ArrayUtil.mergeCollections(own, ext, PsiField.ARRAY_FACTORY);
    }

    @NotNull
    private <T extends PsiMember> List<T> internMembers(List<T> members) {
        return ContainerUtil.map(members, this::internMember);
    }

    @SuppressWarnings("unchecked")
    private <T extends PsiMember> T internMember(T m) {
        if (m == null) return null;
        long modCount = 0;
        for (ModificationTracker tracker : myModificationTrackers) {
            modCount += tracker.getModificationCount();
        }

        synchronized (myInterner) {
            Pair<Long, Interner<PsiMember>> pair = myInterner.get();
            if (pair == null || pair.first.longValue() != modCount) {
                myInterner.set(pair = Pair.create(modCount, Interner.createWeakInterner()));
            }

            return (T) pair.second.intern(m);
        }
    }

    @NotNull
    private PsiMethod[] calcMethods() {
        List<PsiMethod> own = myClass.getOwnMethods();
        List<PsiMethod> ext = internMembers(PsiAugmentProvider.collectAugments(myClass, PsiMethod.class, null));
        return ArrayUtil.mergeCollections(own, ext, PsiMethod.ARRAY_FACTORY);
    }

    @NotNull
    private PsiClass[] calcInnerClasses() {
        List<PsiClass> own = myClass.getOwnInnerClasses();
        List<PsiClass> ext = internMembers(PsiAugmentProvider.collectAugments(myClass, PsiClass.class, null));
        return ArrayUtil.mergeCollections(own, ext, PsiClass.ARRAY_FACTORY);
    }

    @NotNull
    private Map<String, PsiField> getFieldsMap() {
        Map<String, PsiField> cachedFields = new HashMap<>();
        for (PsiField field : myClass.getOwnFields()) {
            String name = field.getName();
            cachedFields.putIfAbsent(name, field);
        }

        return ConcurrentFactoryMap.createMap(name -> {
            PsiField result = cachedFields.get(name);
            return result != null ? result :
                   internMember(ContainerUtil.getFirstItem(PsiAugmentProvider.collectAugments(myClass, PsiField.class, name)));
        });
    }

    @NotNull
    private Map<String, PsiMethod[]> getMethodsMap() {
        List<PsiMethod> ownMethods = myClass.getOwnMethods();
        return ConcurrentFactoryMap.createMap(name -> JBIterable
                .from(ownMethods)
                .filter(m -> name.equals(m.getName()))
                .append(internMembers(PsiAugmentProvider.collectAugments(myClass, PsiMethod.class, name)))
                .toArray(PsiMethod.EMPTY_ARRAY));
    }

    @NotNull
    private Map<String, PsiClass> getInnerClassesMap() {
        Map<String, PsiClass> cachedInners = new HashMap<>();
        for (PsiClass psiClass : myClass.getOwnInnerClasses()) {
            String name = psiClass.getName();
            if (name == null) {
                Logger.getInstance(ClassInnerStuffCache.class).error("$psiClass has no name");
            }
            else if (!(psiClass instanceof ExternallyDefinedPsiElement) || !cachedInners.containsKey(name)) {
                cachedInners.put(name, psiClass);
            }
        }

        return ConcurrentFactoryMap.createMap(name -> {
            PsiClass result = cachedInners.get(name);
            return result != null
                   ? result
                   : internMember(ContainerUtil.getFirstItem(PsiAugmentProvider.collectAugments(myClass, PsiClass.class, name)));
        });
    }
}