/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.light.LightMethod;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.NameHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.intellij.util.ObjectUtils.notNull;

// Copy of ClassInnerStuffCache with custom list of caching dependencies
@SuppressWarnings({"WeakerAccess", "Java8MapApi"})
public class KotlinClassInnerStuffCache {
    private final PsiExtensibleClass myClass;
    private final SimpleModificationTracker myTracker = new SimpleModificationTracker();
    private final List<Object> dependencies;

    public KotlinClassInnerStuffCache(@NotNull PsiExtensibleClass aClass, @NotNull List<Object> externalDependencies) {
        myClass = aClass;

        dependencies = new SmartList<>();
        dependencies.addAll(externalDependencies);
        dependencies.add(myTracker);
    }

    @NotNull
    public PsiMethod[] getConstructors() {
        return copy(CachedValuesManager.getCachedValue(myClass, () -> makeResult(PsiImplUtil.getConstructors(myClass))));
    }

    @NotNull
    public PsiField[] getFields() {
        return copy(CachedValuesManager.getCachedValue(myClass, () -> makeResult(getAllFields())));
    }

    @NotNull
    public PsiMethod[] getMethods() {
        return copy(CachedValuesManager.getCachedValue(myClass, () -> makeResult(getAllMethods())));
    }

    @NotNull
    public PsiClass[] getInnerClasses() {
        return copy(CachedValuesManager.getCachedValue(myClass, () -> makeResult(getAllInnerClasses())));
    }

    @Nullable
    public PsiField findFieldByName(String name, boolean checkBases) {
        if (checkBases) {
            return PsiClassImplUtil.findFieldByName(myClass, name, true);
        }
        else {
            return CachedValuesManager.getCachedValue(myClass, () -> makeResult(getFieldsMap())).get(name);
        }
    }

    @NotNull
    public PsiMethod[] findMethodsByName(String name, boolean checkBases) {
        if (checkBases) {
            return PsiClassImplUtil.findMethodsByName(myClass, name, true);
        }
        else {
            return copy(notNull(CachedValuesManager.getCachedValue(myClass, () -> makeResult(getMethodsMap())).get(name), PsiMethod.EMPTY_ARRAY));
        }
    }

    @Nullable
    public PsiClass findInnerClassByName(String name, boolean checkBases) {
        if (checkBases) {
            return PsiClassImplUtil.findInnerByName(myClass, name, true);
        }
        else {
            return CachedValuesManager.getCachedValue(myClass, () -> makeResult(getInnerClassesMap())).get(name);
        }
    }

    @Nullable
    public PsiMethod getValuesMethod() {
        return myClass.isEnum() && myClass.getName() != null ? CachedValuesManager.getCachedValue(myClass, () -> makeResult(makeValuesMethod())) : null;
    }

    @Nullable
    public PsiMethod getValueOfMethod() {
        return myClass.isEnum() && myClass.getName() != null ? CachedValuesManager.getCachedValue(myClass, () -> makeResult(makeValueOfMethod())) : null;
    }

    private static <T> T[] copy(T[] value) {
        return value.length == 0 ? value : value.clone();
    }

    // This method is modified
    private <T> CachedValueProvider.Result<T> makeResult(T value) {
        return CachedValueProvider.Result.create(value, dependencies);
    }

    @NotNull
    private PsiField[] getAllFields() {
        List<PsiField> own = myClass.getOwnFields();
        List<PsiField> ext = PsiAugmentProvider.collectAugments(myClass, PsiField.class);
        return ArrayUtil.mergeCollections(own, ext, PsiField.ARRAY_FACTORY);
    }

    @NotNull
    private PsiMethod[] getAllMethods() {
        List<PsiMethod> own = myClass.getOwnMethods();
        List<PsiMethod> ext = PsiAugmentProvider.collectAugments(myClass, PsiMethod.class);
        return ArrayUtil.mergeCollections(own, ext, PsiMethod.ARRAY_FACTORY);
    }

    @NotNull
    private PsiClass[] getAllInnerClasses() {
        List<PsiClass> own = myClass.getOwnInnerClasses();
        List<PsiClass> ext = PsiAugmentProvider.collectAugments(myClass, PsiClass.class);
        return ArrayUtil.mergeCollections(own, ext, PsiClass.ARRAY_FACTORY);
    }

    @NotNull
    private Map<String, PsiField> getFieldsMap() {
        PsiField[] fields = getFields();
        if (fields.length == 0) return Collections.emptyMap();

        Map<String, PsiField> cachedFields = new THashMap<>();
        for (PsiField field : fields) {
            String name = field.getName();
            if (!cachedFields.containsKey(name)) {
                cachedFields.put(name, field);
            }
        }
        return cachedFields;
    }

    @NotNull
    private Map<String, PsiMethod[]> getMethodsMap() {
        PsiMethod[] methods = getMethods();
        if (methods.length == 0) return Collections.emptyMap();

        Map<String, List<PsiMethod>> collectedMethods = ContainerUtil.newHashMap();
        for (PsiMethod method : methods) {
            List<PsiMethod> list = collectedMethods.get(method.getName());
            if (list == null) {
                collectedMethods.put(method.getName(), list = ContainerUtil.newSmartList());
            }
            list.add(method);
        }

        Map<String, PsiMethod[]> cachedMethods = ContainerUtil.newTroveMap();
        for (Map.Entry<String, List<PsiMethod>> entry : collectedMethods.entrySet()) {
            List<PsiMethod> list = entry.getValue();
            cachedMethods.put(entry.getKey(), list.toArray(PsiMethod.EMPTY_ARRAY));
        }
        return cachedMethods;
    }

    @NotNull
    private Map<String, PsiClass> getInnerClassesMap() {
        PsiClass[] classes = getInnerClasses();
        if (classes.length == 0) return Collections.emptyMap();

        Map<String, PsiClass> cachedInners = new THashMap<>();
        for (PsiClass psiClass : classes) {
            String name = psiClass.getName();
            if (name == null) {
                Logger.getInstance(KotlinClassInnerStuffCache.class).error(psiClass);
            }
            else if (!(psiClass instanceof ExternallyDefinedPsiElement) || !cachedInners.containsKey(name)) {
                cachedInners.put(name, psiClass);
            }
        }
        return cachedInners;
    }

    private PsiMethod makeValuesMethod() {
        return getSyntheticMethod("public static " + myClass.getName() + "[] values() { }");
    }

    private PsiMethod makeValueOfMethod() {
        return getSyntheticMethod("public static " + myClass.getName() + " valueOf(java.lang.String name) throws java.lang.IllegalArgumentException { }");
    }

    private PsiMethod getSyntheticMethod(String text) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(myClass.getProject());
        PsiMethod method = factory.createMethodFromText(text, myClass);
        return new LightMethod(myClass.getManager(), method, myClass) {
            @Override
            public int getTextOffset() {
                return myClass.getTextOffset();
            }
        };
    }

    public void dropCaches() {
        myTracker.incModificationCount();
    }

    private static final String VALUES_METHOD = "values";
    private static final String VALUE_OF_METHOD = "valueOf";

    // Copy of PsiClassImplUtil.processDeclarationsInEnum for own cache class
    public static boolean processDeclarationsInEnum(@NotNull PsiScopeProcessor processor,
            @NotNull ResolveState state,
            @NotNull KotlinClassInnerStuffCache innerStuffCache) {
        ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);
        if (classHint == null || classHint.shouldProcess(ElementClassHint.DeclarationKind.METHOD)) {
            NameHint nameHint = processor.getHint(NameHint.KEY);
            if (nameHint == null || VALUES_METHOD.equals(nameHint.getName(state))) {
                PsiMethod method = innerStuffCache.getValuesMethod();
                if (method != null && !processor.execute(method, ResolveState.initial())) return false;
            }
            if (nameHint == null || VALUE_OF_METHOD.equals(nameHint.getName(state))) {
                PsiMethod method = innerStuffCache.getValueOfMethod();
                if (method != null && !processor.execute(method, ResolveState.initial())) return false;
            }
        }

        return true;
    }
}
