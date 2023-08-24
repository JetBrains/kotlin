/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiSuperMethodImplUtil;
import com.intellij.psi.impl.light.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Interner;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin;
import org.jetbrains.kotlin.asJava.elements.KtLightMethod;
import org.jetbrains.kotlin.asJava.elements.KtLightParameter;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtParameter;

import java.util.*;

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
    private final boolean myGenerateEnumMethods;

    public ClassInnerStuffCache(
            @NotNull KtExtensibleLightClass aClass,
            boolean generateEnumMethods,
            @NotNull List<ModificationTracker> modificationTrackers
    ) {
        myGenerateEnumMethods = generateEnumMethods;
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

    @Nullable
    private PsiMethod getValuesMethod() {
        return isEnum() ? internMember(CachedValuesManager.getCachedValue(
                myClass,
                () -> CachedValueProvider.Result.create(
                        makeValuesMethod(myClass),
                        myModificationTrackers
                )
        )) : null;
    }

    @Nullable
    private PsiMethod getValueOfMethod() {
        return isEnum() ? internMember(CachedValuesManager.getCachedValue(
                myClass,
                () -> CachedValueProvider.Result.create(
                        makeValueOfMethod(myClass),
                        myModificationTrackers
                )
        )) : null;
    }

    private boolean isEnum() {
        return myGenerateEnumMethods && myClass.isEnum();
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
        if (isEnum()) {
            ext = new ArrayList<>(ext);
            ContainerUtil.addIfNotNull(ext, getValuesMethod());
            ContainerUtil.addIfNotNull(ext, getValueOfMethod());
        }
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
                .append("values".equals(name) ? getValuesMethod() : null)
                .append("valueOf".equals(name) ? getValueOfMethod() : null)
                .append(internMembers(PsiAugmentProvider.collectAugments(myClass, PsiMethod.class, name)))
                .toArray(PsiMethod.EMPTY_ARRAY));
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
            return result != null
                   ? result
                   : internMember(ContainerUtil.getFirstItem(PsiAugmentProvider.collectAugments(myClass, PsiClass.class, name)));
        });
    }

    private static PsiMethod makeValuesMethod(KtExtensibleLightClass enumClass) {
        return new EnumSyntheticMethod(enumClass, EnumMethodKind.Values);
    }

    private static PsiMethod makeValueOfMethod(KtExtensibleLightClass enumClass) {
        return new EnumSyntheticMethod(enumClass, EnumMethodKind.ValueOf);
    }

    private enum EnumMethodKind {
        ValueOf,
        Values,
    }

    private static class EnumSyntheticMethod extends LightElement implements PsiMethod, SyntheticElement, KtLightMethod {
        private final KtExtensibleLightClass myClass;
        private final EnumMethodKind myKind;
        private final PsiType myReturnType;
        private final LightParameterListBuilder myParameterList;
        private final LightModifierList myModifierList;

        EnumSyntheticMethod(@NotNull KtExtensibleLightClass enumClass, EnumMethodKind kind) {
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

        private @NotNull PsiAnnotation[] createNotNullAnnotation() {
            return new PsiAnnotation[] {
                    PsiElementFactory.getInstance(getProject()).createAnnotationFromText(NOT_NULL_ANNOTATION_QUALIFIER, myClass)
            };
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
                private final PsiAnnotation[] notNullAnnotations = createNotNullAnnotation();

                @Override
                @NotNull
                public PsiAnnotation[] getAnnotations() {
                    return ClassInnerStuffCache.copy(notNullAnnotations);
                }

                @Override
                public PsiAnnotation findAnnotation(@NotNull String qualifiedName) {
                    PsiAnnotation notNullAnnotation = notNullAnnotations[0];
                    return qualifiedName == notNullAnnotation.getQualifiedName() ? notNullAnnotation : null;
                }

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
                LightParameter parameter = new MyKtLightParameter(string, this);
                parameters.addParameter(parameter);
            }

            return parameters;
        }

        private static final class MyKtLightParameter extends LightParameter implements KtLightParameter {
            private final @NotNull KtLightMethod myMethod;

            MyKtLightParameter(@NotNull PsiType type, @NotNull KtLightMethod declarationScope) {
                super("name", type, declarationScope, declarationScope.getLanguage(), false);
                myMethod = declarationScope;
            }

            @Nullable
            @Override
            public KtParameter getKotlinOrigin() {
                return null;
            }

            @NotNull
            @Override
            public KtLightMethod getMethod() {
                return myMethod;
            }

            @Override
            public PsiElement getParent() {
                return myMethod;
            }

            @Override
            public PsiFile getContainingFile() {
                return myMethod.getContainingFile();
            }

            @Override
            public String getText() {
                return getName();
            }

            @Override
            public TextRange getTextRange() {
                return TextRange.EMPTY_RANGE;
            }
        }

        @Override
        public int getTextOffset() {
            return myClass.getTextOffset();
        }

        @Override
        public String toString() {
            return myClass.getText();
        }

        @Override
        public String getText() {
            return "";
        }

        @Override
        public TextRange getTextRange() {
            return TextRange.EMPTY_RANGE;
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
        public @NotNull KtExtensibleLightClass getContainingClass() {
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
            LightReferenceListBuilder throwsList = new LightReferenceListBuilder(
                    myManager,
                    getLanguage(),
                    PsiReferenceList.Role.THROWS_LIST
            );

            if (myKind == EnumMethodKind.ValueOf) {
                throwsList.addReference(IllegalArgumentException.class.getName());
                throwsList.addReference(NullPointerException.class.getName());
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
        @NotNull
        public PsiMethod[] findSuperMethods() {
            return PsiSuperMethodImplUtil.findSuperMethods(this);
        }

        @Override
        @NotNull
        public PsiMethod[] findSuperMethods(boolean checkAccess) {
            return PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess);
        }

        @Override
        @NotNull
        public PsiMethod[] findSuperMethods(PsiClass parentClass) {
            return PsiSuperMethodImplUtil.findSuperMethods(this, parentClass);
        }

        @Override
        public @NotNull List<MethodSignatureBackedByPsiMethod> findSuperMethodSignaturesIncludingStatic(boolean checkAccess) {
            return PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess);
        }

        @Override
        @Deprecated
        public @Nullable PsiMethod findDeepestSuperMethod() {
            return PsiSuperMethodImplUtil.findDeepestSuperMethod(this);
        }

        @Override
        @NotNull
        public PsiMethod[] findDeepestSuperMethods() {
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
        public @Nullable PsiAnnotationMemberValue getDefaultValue() {
            return null;
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
        @NotNull
        public PsiTypeParameter[] getTypeParameters() {
            return PsiTypeParameter.EMPTY_ARRAY;
        }

        @Override
        public boolean isMangled() {
            return false;
        }

        @Nullable
        @Override
        public KtDeclaration getKotlinOrigin() {
            return null;
        }

        @Nullable
        @Override
        public LightMemberOrigin getLightMemberOrigin() {
            return null;
        }
    }
}