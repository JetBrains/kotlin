/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.light.AbstractLightClass;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.plugin.JetLanguage;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lexer.JetTokens.*;

public class KotlinLightClassForExplicitDeclaration extends AbstractLightClass implements KotlinLightClass, JetJavaMirrorMarker {
    private final static Key<CachedValue<PsiJavaFileStub>> JAVA_API_STUB = Key.create("JAVA_API_STUB");

    @Nullable
    public static KotlinLightClassForExplicitDeclaration create(@NotNull PsiManager manager, @NotNull FqName qualifiedName, @NotNull JetClassOrObject classOrObject) {
        if (LightClassUtil.belongsToKotlinBuiltIns((JetFile) classOrObject.getContainingFile())) {
            return null;
        }
        return new KotlinLightClassForExplicitDeclaration(manager, qualifiedName, classOrObject);
    }

    private final FqName classFqName; // FqName of (possibly inner) class
    private final JetClassOrObject classOrObject;
    private PsiClass delegate;

    @Nullable
    private PsiModifierList modifierList;

    private KotlinLightClassForExplicitDeclaration(
            @NotNull PsiManager manager,
            @NotNull FqName name,
            @NotNull JetClassOrObject classOrObject
    ) {
        super(manager, JetLanguage.INSTANCE);
        this.classFqName = name;
        this.classOrObject = classOrObject;
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return classFqName;
    }

    @NotNull
    @Override
    public PsiElement copy() {
        return new KotlinLightClassForExplicitDeclaration(getManager(), classFqName, classOrObject);
    }

    @NotNull
    @Override
    public PsiClass getDelegate() {
        if (delegate == null) {
            JetClassOrObject outermostClassOrObject = getOutermostClassOrObject(classOrObject);
            assert outermostClassOrObject != null : "Attempt to build a light class for a local class: " + classOrObject.getText();
            PsiJavaFileStub javaFileStub = CachedValuesManager.getManager(getProject()).getCachedValue(
                    outermostClassOrObject,
                    JAVA_API_STUB,
                    KotlinJavaFileStubProvider.createForDeclaredTopLevelClass(outermostClassOrObject),
                    /*trackValue = */false);

            PsiClass psiClass = LightClassUtil.findClass(classFqName, javaFileStub);
            if (psiClass == null) {
                throw new IllegalStateException("Class was not found " + classFqName + " in " + outermostClassOrObject.getText());
            }
            delegate = psiClass;
        }

        return delegate;
    }

    @Nullable
    private static JetClassOrObject getOutermostClassOrObject(@NotNull JetClassOrObject classOrObject) {
        JetClassOrObject current = classOrObject;
        while (true) {
            PsiElement parent = current.getParent();
            assert classOrObject.getParent() != null : "Class with no parent: " + classOrObject.getText();

            if (parent instanceof PsiFile) {
                return current;
            }
            if (parent instanceof JetClassObject) {
                // current class IS the class object declaration
                parent = parent.getParent();
                assert parent instanceof JetClassBody : "Parent of class object is not a class body: " + parent;
            }
            if (!(parent instanceof JetClassBody)) {
                // It is a local class, no legitimate outer
                return null;
            }

            current = (JetClassOrObject) parent.getParent();
        }
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
    public Icon getElementIcon(final int flags) {
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
    public String getName() {
        return classFqName.shortName().getName();
    }

    @Nullable
    @Override
    public String getQualifiedName() {
        return classFqName.getFqName();
    }

    @NotNull
    @Override
    public PsiModifierList getModifierList() {
        if (modifierList == null) {
            modifierList = new LightModifierList(getManager(), JetLanguage.INSTANCE, computeModifiers());
        }
        return modifierList;
    }

    @NotNull
    private String[] computeModifiers() {
        JetModifierList jetModifierList = classOrObject.getModifierList();
        if (jetModifierList == null) {
            return new String[0];
        }
        Collection<String> psiModifiers = Sets.newHashSet();

        // PUBLIC, PROTECTED, PRIVATE, ABSTRACT, FINAL
        List<Pair<JetKeywordToken, String>> jetTokenToPsiModifier = Lists.newArrayList(
                Pair.create(PUBLIC_KEYWORD, PsiModifier.PUBLIC),
                Pair.create(INTERNAL_KEYWORD, PsiModifier.PUBLIC),
                Pair.create(PROTECTED_KEYWORD, PsiModifier.PROTECTED),
                Pair.create(PRIVATE_KEYWORD, PsiModifier.PRIVATE),
                Pair.create(ABSTRACT_KEYWORD, PsiModifier.ABSTRACT),
                Pair.create(FINAL_KEYWORD, PsiModifier.FINAL));

        for (Pair<JetKeywordToken, String> tokenAndModifier : jetTokenToPsiModifier) {
            if (jetModifierList.hasModifier(tokenAndModifier.first)) {
                psiModifiers.add(tokenAndModifier.second);
            }
        }

        if (!psiModifiers.contains(PsiModifier.PRIVATE) && !psiModifiers.contains(PsiModifier.PROTECTED)) {
            psiModifiers.add(PsiModifier.PUBLIC); // For internal (default) visibility
        }

        // FINAL
        if (!jetModifierList.hasModifier(OPEN_KEYWORD) && !jetModifierList.hasModifier(ABSTRACT_KEYWORD)) {
            psiModifiers.add(PsiModifier.FINAL);
        }

        // STATIC
        if (classOrObject.getParent() != classOrObject.getContainingFile()
                //TODO: && !jetModifierList.hasModifier(INNER_KEYWORD)
                ) {
            psiModifiers.add(PsiModifier.STATIC);
        }

        return psiModifiers.toArray(new String[psiModifiers.size()]);
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
        String deprecatedName = deprecatedAnnotation.getName().getName();
        FqNameUnsafe deprecatedFqName = DescriptorUtils.getFQName(deprecatedAnnotation);

        for (JetAnnotationEntry annotationEntry : jetModifierList.getAnnotationEntries()) {
            JetTypeReference typeReference = annotationEntry.getTypeReference();
            if (typeReference == null) continue;

            JetTypeElement typeElement = typeReference.getTypeElement();
            if (typeElement == null) continue;

            // typeElement.getText() is either
            //   simple name => we just compare it to "deprecated"
            //   qualified name => we compare to FqName, there may be spaces, comments etc, we do not support these cases
            //   function type, etc => comparisons below fail
            String text = typeElement.getText();
            if (deprecatedFqName.getFqName().equals(text)) return true;
            if (deprecatedName.equals(text)) return true;
        }
        return false;
    }

    @Override
    public boolean isInterface() {
        return classOrObject instanceof JetClass && ((JetClass) classOrObject).isTrait();
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
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        return super.setName(name); // TODO
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
}
