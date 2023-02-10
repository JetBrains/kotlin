/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiSuperMethodImplUtil;
import com.intellij.psi.impl.light.*;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Interner;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.util.ObjectUtils.notNull;

/**
 * Copy-pasted and updated from com.intellij.psi.impl.source.ClassInnerStuffCache
 *
 * @see com.intellij.psi.impl.source.ClassInnerStuffCache
 */
public final class ClassInnerStuffCache {
    private final PsiExtensibleClass myClass;
    private final Ref<Pair<Long, Interner<PsiMember>>> myInterner = Ref.create();

    public ClassInnerStuffCache(@NotNull PsiExtensibleClass aClass) {
        myClass = aClass;
    }

    public PsiMethod @NotNull [] getConstructors() {
        return copy(CachedValuesManager.getProjectPsiDependentCache(myClass, PsiImplUtil::getConstructors));
    }

    public PsiField @NotNull [] getFields() {
        return copy(CachedValuesManager.getProjectPsiDependentCache(myClass, __ -> calcFields()));
    }

    public PsiMethod @NotNull [] getMethods() {
        return copy(CachedValuesManager.getProjectPsiDependentCache(myClass, __ -> calcMethods()));
    }

    public PsiClass @NotNull [] getInnerClasses() {
        return copy(CachedValuesManager.getProjectPsiDependentCache(myClass, __ -> calcInnerClasses()));
    }

    public PsiRecordComponent @NotNull [] getRecordComponents() {
        return copy(CachedValuesManager.getProjectPsiDependentCache(myClass, __ -> calcRecordComponents()));
    }

    @Nullable
    public PsiField findFieldByName(String name, boolean checkBases) {
        if (checkBases) {
            return PsiClassImplUtil.findFieldByName(myClass, name, true);
        }
        else {
            return CachedValuesManager.getProjectPsiDependentCache(myClass, __ -> getFieldsMap()).get(name);
        }
    }

    public PsiMethod @NotNull [] findMethodsByName(String name, boolean checkBases) {
        if (checkBases) {
            return PsiClassImplUtil.findMethodsByName(myClass, name, true);
        }
        else {
            return copy(notNull(CachedValuesManager.getProjectPsiDependentCache(myClass, __ -> getMethodsMap()).get(name), PsiMethod.EMPTY_ARRAY));
        }
    }

    @Nullable
    public PsiClass findInnerClassByName(String name, boolean checkBases) {
        if (checkBases) {
            return PsiClassImplUtil.findInnerByName(myClass, name, true);
        }
        else {
            return CachedValuesManager.getProjectPsiDependentCache(myClass, __ -> getInnerClassesMap()).get(name);
        }
    }

    @Nullable
    PsiMethod getValuesMethod() {
        return myClass.isEnum() && !isAnonymousClass() && !classNameIsSealed()
               ? internMember(CachedValuesManager.getProjectPsiDependentCache(myClass, ClassInnerStuffCache::makeValuesMethod))
               : null;
    }

    private boolean classNameIsSealed() {
        return PsiKeyword.SEALED.equals(myClass.getName()) && PsiUtil.getLanguageLevel(myClass).isAtLeast(LanguageLevel.JDK_17);
    }

    @Nullable
    private PsiMethod getValueOfMethod() {
        return myClass.isEnum() && !isAnonymousClass()
               ? internMember(CachedValuesManager.getProjectPsiDependentCache(myClass, ClassInnerStuffCache::makeValueOfMethod))
               : null;
    }

    private boolean isAnonymousClass() {
        return myClass.getName() == null || myClass instanceof PsiAnonymousClass;
    }

    private static <T> T[] copy(T[] value) {
        return value.length == 0 ? value : value.clone();
    }

    private PsiField @NotNull [] calcFields() {
        List<PsiField> own = myClass.getOwnFields();
        List<PsiField> ext = internMembers(PsiAugmentProvider.collectAugments(myClass, PsiField.class, null));
        return ArrayUtil.mergeCollections(own, ext, PsiField.ARRAY_FACTORY);
    }

    @NotNull
    private <T extends PsiMember> List<T> internMembers(List<T> members) {
        return ContainerUtil.map(members, this::internMember);
    }

    private <T extends PsiMember> T internMember(T m) {
        if (m == null) return null;
        long modCount = myClass.getManager().getModificationTracker().getModificationCount();
        synchronized (myInterner) {
            Pair<Long, Interner<PsiMember>> pair = myInterner.get();
            if (pair == null || pair.first.longValue() != modCount) {
                myInterner.set(pair = Pair.create(modCount, Interner.createWeakInterner()));
            }
            //noinspection unchecked
            return (T) pair.second.intern(m);
        }
    }

    private PsiMethod @NotNull [] calcMethods() {
        List<PsiMethod> own = myClass.getOwnMethods();
        List<PsiMethod> ext = internMembers(PsiAugmentProvider.collectAugments(myClass, PsiMethod.class, null));
        if (myGenerateEnumMethods && myClass.isEnum()) {
            ext = new ArrayList<>(ext);
            ContainerUtil.addIfNotNull(ext, getValuesMethod());
            ContainerUtil.addIfNotNull(ext, getValueOfMethod());
        }
        return ArrayUtil.mergeCollections(own, ext, PsiMethod.ARRAY_FACTORY);
    }

    private PsiClass @NotNull [] calcInnerClasses() {
        List<PsiClass> own = myClass.getOwnInnerClasses();
        List<PsiClass> ext = internMembers(PsiAugmentProvider.collectAugments(myClass, PsiClass.class, null));
        return ArrayUtil.mergeCollections(own, ext, PsiClass.ARRAY_FACTORY);
    }

    private PsiRecordComponent @NotNull [] calcRecordComponents() {
        PsiRecordHeader header = myClass.getRecordHeader();
        return header == null ? PsiRecordComponent.EMPTY_ARRAY : header.getRecordComponents();
    }

    @NotNull
    private Map<String, PsiField> getFieldsMap() {
        Map<String, PsiField> cachedFields = new HashMap<>();
        for (PsiField field : myClass.getOwnFields()) {
            String name = field.getName();
            if (!cachedFields.containsKey(name)) {
                cachedFields.put(name, field);
            }
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
        return ConcurrentFactoryMap.createMap(name -> {
            return JBIterable
                    .from(ownMethods).filter(m -> name.equals(m.getName()))
                    .append("values".equals(name) ? getValuesMethod() : null)
                    .append("valueOf".equals(name) ? getValueOfMethod() : null)
                    .append(internMembers(PsiAugmentProvider.collectAugments(myClass, PsiMethod.class, name)))
                    .toArray(PsiMethod.EMPTY_ARRAY);
        });
    }

    @NotNull
    private Map<String, PsiClass> getInnerClassesMap() {
        Map<String, PsiClass> cachedInners = new HashMap<>();
        for (PsiClass psiClass : myClass.getOwnInnerClasses()) {
            String name = psiClass.getName();
            if (name == null) {
                Logger.getInstance(ClassInnerStuffCache.class).error(psiClass);
            }
            else if (!(psiClass instanceof ExternallyDefinedPsiElement) || !cachedInners.containsKey(name)) {
                cachedInners.put(name, psiClass);
            }
        }
        return ConcurrentFactoryMap.createMap(name -> {
            PsiClass result = cachedInners.get(name);
            return result != null ? result :
                   internMember(ContainerUtil.getFirstItem(PsiAugmentProvider.collectAugments(myClass, PsiClass.class, name)));
        });
    }

    private static PsiMethod makeValuesMethod(PsiExtensibleClass enumClass) {
        return new EnumSyntheticMethod(enumClass, EnumMethodKind.Values);
    }

    private static PsiMethod makeValueOfMethod(PsiExtensibleClass enumClass) {
        return new EnumSyntheticMethod(enumClass, EnumMethodKind.ValueOf);
    }

    private enum EnumMethodKind {
        ValueOf,
        Values,
    }

    private static class EnumSyntheticMethod extends LightElement implements PsiMethod, SyntheticElement {
        private final PsiClass myClass;
        private final EnumMethodKind myKind;
        private final PsiType myReturnType;
        private final LightParameterListBuilder myParameterList;
        private final LightModifierList myModifierList;

        EnumSyntheticMethod(@NotNull PsiClass enumClass, EnumMethodKind kind) {
            super(enumClass.getManager(), enumClass.getLanguage());
            myClass = enumClass;
            myKind = kind;
            myReturnType = createReturnType();
            myParameterList = createParameterList();
            myModifierList = createModifierList();
        }

        @Override
        public void accept(@NotNull PsiElementVisitor visitor) {
            if (visitor instanceof JavaElementVisitor) {
                ((JavaElementVisitor) visitor).visitMethod(this);
            }
            else {
                visitor.visitElement(this);
            }
        }

        private @NotNull PsiType createReturnType() {
            PsiClassType type = JavaPsiFacade.getElementFactory(getProject()).createType(myClass);
            if (myKind == EnumMethodKind.Values) {
                return type.createArrayType();
            }
            return type;
        }

        @NotNull
        private LightModifierList createModifierList() {
            return new LightModifierList(myManager, getLanguage(), PsiModifier.PUBLIC, PsiModifier.STATIC) {
                @Override
                public PsiElement getParent() {
                    return EnumSyntheticMethod.this;
                }
            };
        }

        @NotNull
        private LightParameterListBuilder createParameterList() {
            LightParameterListBuilder parameters = new LightParameterListBuilder(myManager, getLanguage());
            if (myKind == EnumMethodKind.ValueOf) {
                PsiClassType string = PsiType.getJavaLangString(myManager, GlobalSearchScope.allScope(getProject()));
                LightParameter parameter = new LightParameter("name", string, this, getLanguage(), false);
                parameters.addParameter(parameter);
            }
            return parameters;
        }

        @Override
        public int getTextOffset() {
            return myClass.getTextOffset();
        }

        @Override
        public String toString() {
            return null;
        }

        @Override
        public boolean equals(Object another) {
            return this == another ||
                   another instanceof EnumSyntheticMethod &&
                   myClass.equals(((EnumSyntheticMethod) another).myClass) &&
                   myKind == ((EnumSyntheticMethod) another).myKind;
        }

        @Override
        public int hashCode() {
            return Objects.hash(myClass, myKind);
        }

        @Override
        public boolean isDeprecated() {
            return false;
        }

        @Override
        public @Nullable PsiDocComment getDocComment() {
            return null;
        }

        @Override
        public @Nullable PsiClass getContainingClass() {
            return myClass;
        }

        @Override
        public @Nullable PsiType getReturnType() {
            return myReturnType;
        }

        @Override
        public @Nullable PsiTypeElement getReturnTypeElement() {
            return null;
        }

        @Override
        public @NotNull PsiParameterList getParameterList() {
            return myParameterList;
        }

        @Override
        public @NotNull PsiReferenceList getThrowsList() {
            LightReferenceListBuilder throwsList = new LightReferenceListBuilder(myManager, getLanguage(), PsiReferenceList.Role.THROWS_LIST);
            if (myKind == EnumMethodKind.ValueOf) {
                throwsList.addReference("java.lang.IllegalArgumentException");
            }

            return throwsList;
        }

        @Override
        public @Nullable PsiCodeBlock getBody() {
            return null;
        }

        @Override
        public boolean isConstructor() {
            return false;
        }

        @Override
        public boolean isVarArgs() {
            return false;
        }

        @Override
        public @NotNull MethodSignature getSignature(@NotNull PsiSubstitutor substitutor) {
            return MethodSignatureBackedByPsiMethod.create(this, substitutor);
        }

        @Override
        public @Nullable PsiIdentifier getNameIdentifier() {
            return new LightIdentifier(myManager, getName());
        }

        @Override
        public @NotNull String getName() {
            if (myKind == EnumMethodKind.ValueOf) {
                return "valueOf";
            }
            return "values";
        }

        @Override
        public PsiMethod @NotNull [] findSuperMethods() {
            return PsiSuperMethodImplUtil.findSuperMethods(this);
        }

        @Override
        public PsiMethod @NotNull [] findSuperMethods(boolean checkAccess) {
            return PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess);
        }

        @Override
        public PsiMethod @NotNull [] findSuperMethods(PsiClass parentClass) {
            return PsiSuperMethodImplUtil.findSuperMethods(this, parentClass);
        }

        @Override
        public @NotNull List<MethodSignatureBackedByPsiMethod> findSuperMethodSignaturesIncludingStatic(boolean checkAccess) {
            return PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess);
        }

        @Override
        public @Nullable PsiMethod findDeepestSuperMethod() {
            return PsiSuperMethodImplUtil.findDeepestSuperMethod(this);
        }

        @Override
        public PsiMethod @NotNull [] findDeepestSuperMethods() {
            return PsiMethod.EMPTY_ARRAY;
        }

        @Override
        public @NotNull PsiModifierList getModifierList() {
            return myModifierList;
        }

        @Override
        public boolean hasModifierProperty(@NonNls @NotNull String name) {
            return name.equals(PsiModifier.PUBLIC) || name.equals(PsiModifier.STATIC);
        }

        @Override
        public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
            throw new IncorrectOperationException();
        }

        @Override
        public @NotNull HierarchicalMethodSignature getHierarchicalMethodSignature() {
            return PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this);
        }

        @Override
        public boolean hasTypeParameters() {
            return false;
        }

        @Override
        public @Nullable PsiTypeParameterList getTypeParameterList() {
            return null;
        }

        @Override
        public PsiFile getContainingFile() {
            return myClass.getContainingFile();
        }

        @Override
        public PsiTypeParameter @NotNull [] getTypeParameters() {
            return PsiTypeParameter.EMPTY_ARRAY;
        }
    }
}