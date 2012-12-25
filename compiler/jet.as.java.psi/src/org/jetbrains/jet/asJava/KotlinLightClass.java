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

import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.light.AbstractLightClass;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClassBody;
import org.jetbrains.jet.lang.psi.JetClassObject;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.JetLanguage;

import javax.swing.*;

public class KotlinLightClass extends AbstractLightClass implements JetJavaMirrorMarker {
    private final static Key<CachedValue<PsiJavaFileStub>> JAVA_API_STUB = Key.create("JAVA_API_STUB");

    @Nullable
    public static KotlinLightClass create(@NotNull PsiManager manager, @NotNull FqName qualifiedName, @NotNull JetClassOrObject classOrObject) {
        if (LightClassUtil.belongsToKotlinBuiltIns((JetFile) classOrObject.getContainingFile())) {
            return null;
        }
        return new KotlinLightClass(manager, qualifiedName, classOrObject);
    }

    private final FqName classFqName; // FqName of (possibly inner) class
    private final JetClassOrObject outermostClassOrObject; // outermost parent of this class (may be equal to this class itself)
    private PsiClass delegate;

    private KotlinLightClass(@NotNull PsiManager manager, @NotNull FqName name, @NotNull JetClassOrObject classOrObject) {
        super(manager, JetLanguage.INSTANCE);
        this.classFqName = name;
        this.outermostClassOrObject = getOutermostClassOrObject(classOrObject);
        assert outermostClassOrObject != null : "Attempt to build a light class for a local class: " + classOrObject.getText();
    }

    @NotNull
    @Override
    public PsiElement copy() {
        // It's fine to pass outermostClassOrObject, because getOutermostClassOrObject() will return the same thing
        return new KotlinLightClass(getManager(), classFqName, outermostClassOrObject);
    }

    @NotNull
    @Override
    public PsiClass getDelegate() {
        if (delegate == null) {
            PsiJavaFileStub javaFileStub = CachedValuesManager.getManager(getProject()).getCachedValue(
                    outermostClassOrObject,
                    JAVA_API_STUB,
                    KotlinLightClassProvider.createForDeclaredTopLevelClass(outermostClassOrObject),
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
        return outermostClassOrObject;
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

        KotlinLightClass aClass = (KotlinLightClass) o;

        if (!classFqName.equals(aClass.classFqName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return classFqName.hashCode();
    }
}
