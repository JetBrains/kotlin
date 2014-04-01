/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.asJava;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.light.LightClass;
import com.intellij.psi.impl.light.LightMethod;
import com.intellij.psi.stubs.PsiClassHolderFileStub;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.binding.PsiCodegenPredictor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.jetAsJava.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetModifierKeywordToken;
import org.jetbrains.jet.plugin.JetLanguage;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lexer.JetTokens.*;

public class KotlinLightClassForExplicitDeclaration extends KotlinWrappingLightClass implements JetJavaMirrorMarker {
    private final static Key<CachedValue<OutermostKotlinClassLightClassData>> JAVA_API_STUB = Key.create("JAVA_API_STUB");

    @Nullable
    public static KotlinLightClassForExplicitDeclaration create(@NotNull PsiManager manager, @NotNull JetClassOrObject classOrObject) {
        if (LightClassUtil.belongsToKotlinBuiltIns((JetFile) classOrObject.getContainingFile())) {
            return null;
        }

        String jvmInternalName = getJvmInternalName(classOrObject);
        if (jvmInternalName == null) return null;

        FqName fqName = JvmClassName.byInternalName(jvmInternalName).getFqNameForClassNameWithoutDollars();

        if (classOrObject instanceof JetObjectDeclaration && ((JetObjectDeclaration) classOrObject).isObjectLiteral()) {
            return new KotlinLightClassForAnonymousDeclaration(manager, fqName, classOrObject);
        }
        return new KotlinLightClassForExplicitDeclaration(manager, fqName, classOrObject);
    }

    private static String getJvmInternalName(JetClassOrObject classOrObject) {
        if (JetPsiUtil.isLocal(classOrObject)) {
            LightClassDataForKotlinClass data = getLightClassDataExactly(classOrObject);
            return data != null ? data.getJvmInternalName() : "";
        }
        return PsiCodegenPredictor.getPredefinedJvmInternalName(classOrObject);
    }

    private final FqName classFqName; // FqName of (possibly inner) class
    protected final JetClassOrObject classOrObject;
    private PsiClass delegate;

    private final NullableLazyValue<PsiElement> parent = new NullableLazyValue<PsiElement>() {
        @Nullable
        @Override
        protected PsiElement compute() {
            if (JetPsiUtil.isLocal(classOrObject)) {
                //noinspection unchecked
                PsiElement declaration = JetPsiUtil.getTopmostParentOfTypes(
                        classOrObject,
                        JetNamedFunction.class, JetProperty.class, JetClassInitializer.class, JetParameter.class
                );

                if (declaration instanceof JetParameter) {
                    declaration = PsiTreeUtil.getParentOfType(declaration, JetNamedDeclaration.class);
                }

                if (declaration instanceof JetNamedFunction) {
                    JetNamedFunction function = (JetNamedFunction) declaration;
                    return getParentByPsiMethod(LightClassUtil.getLightClassMethod(function), function.getName(), false);
                }

                // Represent the property as a fake method with the same name
                if (declaration instanceof JetProperty) {
                    JetProperty property = (JetProperty) declaration;
                    return getParentByPsiMethod(LightClassUtil.getLightClassPropertyMethods(property).getGetter(), property.getName(), true);
                }

                if (declaration instanceof JetClassInitializer) {
                    PsiElement parent = declaration.getParent();
                    PsiElement grandparent = parent.getParent();

                    if (parent instanceof JetClassBody && grandparent instanceof JetClassOrObject) {
                        return LightClassUtil.getPsiClass((JetClassOrObject) grandparent);
                    }
                }

                if (declaration instanceof JetClass) {
                    return LightClassUtil.getPsiClass((JetClass) declaration);
                }
            }

            return classOrObject.getParent() == classOrObject.getContainingFile() ? getContainingFile() : getContainingClass();
        }

        private PsiElement getParentByPsiMethod(PsiMethod method, final String name, boolean forceMethodWrapping) {
            if (method == null || name == null) return null;

            PsiClass containingClass = method.getContainingClass();
            if (containingClass == null) return null;

            final String currentFileName = classOrObject.getContainingFile().getName();

            boolean createWrapper = forceMethodWrapping;
            // Use PsiClass wrapper instead of package light class to avoid names like "FooPackage" in Type Hierarchy and related views
            if (containingClass instanceof KotlinLightClassForPackage) {
                containingClass = new LightClass(containingClass, JetLanguage.INSTANCE) {
                    @Nullable
                    @Override
                    public String getName() {
                        return currentFileName;
                    }
                };
                createWrapper = true;
            }

            if (createWrapper) {
                return new LightMethod(myManager, method, containingClass, JetLanguage.INSTANCE) {
                    @Override
                    public PsiElement getParent() {
                        return getContainingClass();
                    }

                    @NotNull
                    @Override
                    public String getName() {
                        return name;
                    }
                };
            }

            return method;
        }
    };

    @Nullable
    private PsiModifierList modifierList;

    private final NullableLazyValue<PsiTypeParameterList> typeParameterList = new NullableLazyValue<PsiTypeParameterList>() {
        @Nullable
        @Override
        protected PsiTypeParameterList compute() {
            return LightClassUtil.buildLightTypeParameterList(KotlinLightClassForExplicitDeclaration.this, classOrObject);
        }
    };

    KotlinLightClassForExplicitDeclaration(
            @NotNull PsiManager manager,
            @NotNull FqName name,
            @NotNull JetClassOrObject classOrObject
    ) {
        super(manager, JetLanguage.INSTANCE);
        this.classFqName = name;
        this.classOrObject = classOrObject;
    }

    @Override
    @NotNull
    public JetClassOrObject getOrigin() {
        return classOrObject;
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return classFqName;
    }

    @NotNull
    @Override
    public PsiElement copy() {
        return new KotlinLightClassForExplicitDeclaration(getManager(), classFqName, (JetClassOrObject) classOrObject.copy());
    }

    @NotNull
    @Override
    public PsiClass getDelegate() {
        if (delegate == null) {
            PsiJavaFileStub javaFileStub = getJavaFileStub();

            PsiClass psiClass = LightClassUtil.findClass(classFqName, javaFileStub);
            if (psiClass == null) {
                JetClassOrObject outermostClassOrObject = getOutermostClassOrObject(classOrObject);
                throw new IllegalStateException("Class was not found " + classFqName + "\n" +
                                                "in " + outermostClassOrObject.getContainingFile().getText() + "\n" +
                                                "stub: \n" + javaFileStub.getPsi().getText());
            }
            delegate = psiClass;
        }

        return delegate;
    }

    @NotNull
    private PsiJavaFileStub getJavaFileStub() {
        return getLightClassData().getJavaFileStub();
    }

    @Nullable
    protected final ClassDescriptor getDescriptor() {
        LightClassDataForKotlinClass data = getLightClassDataExactly(classOrObject);
        return data != null ? data.getDescriptor() : null;
    }

    @NotNull
    private OutermostKotlinClassLightClassData getLightClassData() {
        return getLightClassData(classOrObject);
    }

    @NotNull
    private static OutermostKotlinClassLightClassData getLightClassData(JetClassOrObject classOrObject) {
        JetClassOrObject outermostClassOrObject = getOutermostClassOrObject(classOrObject);
        return CachedValuesManager.getManager(classOrObject.getProject()).getCachedValue(
                outermostClassOrObject,
                JAVA_API_STUB,
                KotlinJavaFileStubProvider.createForDeclaredClass(outermostClassOrObject),
                /*trackValue = */false
        );
    }

    @Nullable
    private static LightClassDataForKotlinClass getLightClassDataExactly(JetClassOrObject classOrObject) {
        OutermostKotlinClassLightClassData data = getLightClassData(classOrObject);
        return data.getClassOrObject().equals(classOrObject) ? data : data.getAllInnerClasses().get(classOrObject);
    }

    @NotNull
    private static JetClassOrObject getOutermostClassOrObject(@NotNull JetClassOrObject classOrObject) {
        JetClassOrObject outermostClass = JetPsiUtil.getOutermostClassOrObject(classOrObject);
        if (outermostClass == null) {
            throw new IllegalStateException("Attempt to build a light class for a local class: " + classOrObject.getText());
        }
        else {
            return outermostClass;
        }
    }

    private final NullableLazyValue<PsiFile> _containingFile = new NullableLazyValue<PsiFile>() {
        @Nullable
        @Override
        protected PsiFile compute() {
            VirtualFile virtualFile = classOrObject.getContainingFile().getVirtualFile();
            assert virtualFile != null : "No virtual file for " + classOrObject.getText();
            return new ClsFileImpl((PsiManagerImpl) getManager(), new ClassFileViewProvider(getManager(), virtualFile)) {
                @NotNull
                @Override
                public String getPackageName() {
                    return ((JetFile) classOrObject.getContainingFile()).getPackageFqName().asString();
                }

                @NotNull
                @Override
                public PsiClassHolderFileStub getStub() {
                    return getJavaFileStub();
                }
            };
        }
    };

    @Override
    public PsiFile getContainingFile() {
        return _containingFile.getValue();
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        return classOrObject;
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return another instanceof PsiClass && Comparing.equal(((PsiClass) another).getQualifiedName(), getQualifiedName());
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public Icon getElementIcon(int flags) {
        throw new UnsupportedOperationException("This should be done byt JetIconProvider");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KotlinLightClassForExplicitDeclaration aClass = (KotlinLightClassForExplicitDeclaration) o;

        if (!classFqName.equals(aClass.classFqName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return classFqName.hashCode();
    }

    @Nullable
    @Override
    public PsiClass getContainingClass() {
        if (classOrObject.getParent() == classOrObject.getContainingFile()) return null;
        return super.getContainingClass();
    }

    @Nullable
    @Override
    public PsiElement getParent() {
        return parent.getValue();
    }

    @Nullable
    @Override
    public PsiTypeParameterList getTypeParameterList() {
        return typeParameterList.getValue();
    }

    @NotNull
    @Override
    public PsiTypeParameter[] getTypeParameters() {
        PsiTypeParameterList typeParameterList = getTypeParameterList();
        return typeParameterList == null ? PsiTypeParameter.EMPTY_ARRAY : typeParameterList.getTypeParameters();
    }

    @Nullable
    @Override
    public String getName() {
        return classFqName.shortName().asString();
    }

    @Nullable
    @Override
    public String getQualifiedName() {
        return classFqName.asString();
    }

    @NotNull
    @Override
    public PsiModifierList getModifierList() {
        if (modifierList == null) {
            modifierList = new KotlinLightModifierList(this.getManager(), computeModifiers()) {
                @Override
                public PsiAnnotationOwner getDelegate() {
                    return KotlinLightClassForExplicitDeclaration.this.getDelegate().getModifierList();
                }
            };
        }
        return modifierList;
    }

    @NotNull
    private String[] computeModifiers() {
        boolean nestedClass = classOrObject.getParent() != classOrObject.getContainingFile();
        Collection<String> psiModifiers = Sets.newHashSet();

        // PUBLIC, PROTECTED, PRIVATE, ABSTRACT, FINAL
        //noinspection unchecked
        List<Pair<JetModifierKeywordToken, String>> jetTokenToPsiModifier = Lists.newArrayList(
                Pair.create(PUBLIC_KEYWORD, PsiModifier.PUBLIC),
                Pair.create(INTERNAL_KEYWORD, PsiModifier.PUBLIC),
                Pair.create(PROTECTED_KEYWORD, PsiModifier.PROTECTED),
                Pair.create(FINAL_KEYWORD, PsiModifier.FINAL));

        for (Pair<JetModifierKeywordToken, String> tokenAndModifier : jetTokenToPsiModifier) {
            if (classOrObject.hasModifier(tokenAndModifier.first)) {
                psiModifiers.add(tokenAndModifier.second);
            }
        }

        if (classOrObject.hasModifier(PRIVATE_KEYWORD)) {
            // Top-level private class has PUBLIC visibility in Java
            // Nested private class has PRIVATE visibility
            psiModifiers.add(nestedClass ? PsiModifier.PRIVATE : PsiModifier.PUBLIC);
        }

        if (!psiModifiers.contains(PsiModifier.PRIVATE) && !psiModifiers.contains(PsiModifier.PROTECTED)) {
            psiModifiers.add(PsiModifier.PUBLIC); // For internal (default) visibility
        }


        // FINAL
        if (isAbstract(classOrObject)) {
            psiModifiers.add(PsiModifier.ABSTRACT);
        }
        else if (!classOrObject.hasModifier(OPEN_KEYWORD)) {
            psiModifiers.add(PsiModifier.FINAL);
        }

        if (nestedClass && !classOrObject.hasModifier(INNER_KEYWORD)) {
            psiModifiers.add(PsiModifier.STATIC);
        }

        return ArrayUtil.toStringArray(psiModifiers);
    }

    private boolean isAbstract(@NotNull JetClassOrObject object) {
        return object.hasModifier(ABSTRACT_KEYWORD) || isInterface();
    }

    @Override
    public boolean hasModifierProperty(@NonNls @NotNull String name) {
        return getModifierList().hasModifierProperty(name);
    }

    @Override
    public boolean isDeprecated() {
        JetModifierList jetModifierList = classOrObject.getModifierList();
        if (jetModifierList == null) {
            return false;
        }

        ClassDescriptor deprecatedAnnotation = KotlinBuiltIns.getInstance().getDeprecatedAnnotation();
        String deprecatedName = deprecatedAnnotation.getName().asString();
        FqNameUnsafe deprecatedFqName = DescriptorUtils.getFqName(deprecatedAnnotation);

        for (JetAnnotationEntry annotationEntry : jetModifierList.getAnnotationEntries()) {
            JetTypeReference typeReference = annotationEntry.getTypeReference();
            if (typeReference == null) continue;

            JetTypeElement typeElement = typeReference.getTypeElement();
            if (!(typeElement instanceof JetUserType)) continue; // If it's not a user type, it's definitely not a ref to deprecated

            FqName fqName = JetPsiUtil.toQualifiedName((JetUserType) typeElement);
            if (fqName == null) continue;

            if (deprecatedFqName.equals(fqName.toUnsafe())) return true;
            if (deprecatedName.equals(fqName.asString())) return true;
        }
        return false;
    }

    @Override
    public boolean isInterface() {
        if (!(classOrObject instanceof JetClass)) return false;
        JetClass jetClass = (JetClass) classOrObject;
        return jetClass.isTrait() || jetClass.isAnnotation();
    }

    @Override
    public boolean isAnnotationType() {
        return classOrObject instanceof JetClass && ((JetClass) classOrObject).isAnnotation();
    }

    @Override
    public boolean isEnum() {
        return classOrObject instanceof JetClass && ((JetClass) classOrObject).isEnum();
    }

    @Override
    public boolean hasTypeParameters() {
        return classOrObject instanceof JetClass && !((JetClass) classOrObject).getTypeParameters().isEmpty();
    }

    @Override
    public boolean isValid() {
        return classOrObject.isValid();
    }

    @Override
    public boolean isInheritor(@NotNull PsiClass baseClass, boolean checkDeep) {
        // Java inheritor check doesn't work when trait (interface in Java) subclasses Java class and for Kotlin local classes
        if (baseClass instanceof KotlinLightClassForExplicitDeclaration || (isInterface() && !baseClass.isInterface())) {
            String qualifiedName;
            if (baseClass instanceof KotlinLightClassForExplicitDeclaration) {
                ClassDescriptor baseDescriptor = ((KotlinLightClassForExplicitDeclaration) baseClass).getDescriptor();
                qualifiedName = baseDescriptor != null ? DescriptorUtils.getFqName(baseDescriptor).asString() : null;
            }
            else {
                qualifiedName = baseClass.getQualifiedName();
            }

            ClassDescriptor thisDescriptor = getDescriptor();
            return qualifiedName != null
                   && thisDescriptor != null
                   && checkSuperTypeByFQName(thisDescriptor, qualifiedName, checkDeep);
        }

        return super.isInheritor(baseClass, checkDeep);
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        throw new IncorrectOperationException("Cannot modify compiled kotlin element");
    }

    @Override
    public String toString() {
        try {
            return KotlinLightClass.class.getSimpleName() + ":" + getQualifiedName();
        }
        catch (Throwable e) {
            return KotlinLightClass.class.getSimpleName() + ":" + e.toString();
        }
    }

    @NotNull
    @Override
    public List<PsiClass> getOwnInnerClasses() {
        // TODO: Should return inner class wrapper
        return Arrays.asList(getDelegate().getInnerClasses());
    }

    private static boolean checkSuperTypeByFQName(@NotNull ClassDescriptor classDescriptor, @NotNull String qualifiedName, Boolean deep) {
        if (CommonClassNames.JAVA_LANG_OBJECT.equals(qualifiedName)) return true;

        if (qualifiedName.equals(DescriptorUtils.getFqName(classDescriptor).asString())) return true;

        for (JetType superType : classDescriptor.getTypeConstructor().getSupertypes()) {
            ClassifierDescriptor superDescriptor = superType.getConstructor().getDeclarationDescriptor();

            if (superDescriptor instanceof ClassDescriptor) {
                if (qualifiedName.equals(DescriptorUtils.getFqName(superDescriptor).asString())) return true;

                if (deep) {
                    if (checkSuperTypeByFQName((ClassDescriptor)superDescriptor, qualifiedName, true)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
