/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes;

import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightReferenceListBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Copy-pasted mostly from com.intellij.psi.impl.light.LightReferenceListBuilder
 */
public class KotlinLightReferenceListBuilder extends LightReferenceListBuilder implements PsiReferenceList {
    private final List<PsiJavaCodeReferenceElement> myRefs = new ArrayList<>();
    private PsiJavaCodeReferenceElement[] myCachedRefs;
    private PsiClassType[] myCachedTypes;
    private final Role myRole;
    private final PsiElementFactory myFactory;

    public KotlinLightReferenceListBuilder(PsiManager manager, Role role) {
        this(manager, JavaLanguage.INSTANCE, role);
    }

    public KotlinLightReferenceListBuilder(PsiManager manager, Language language, Role role) {
        super(manager, language, role);
        myRole = role;
        myFactory = JavaPsiFacade.getElementFactory(getProject());
    }

    @Override
    public void addReference(PsiClass aClass) {
        addReference(aClass.getQualifiedName());
    }

    @Override
    public void addReference(String qualifiedName) {
        final PsiJavaCodeReferenceElement ref = myFactory.createReferenceElementByFQClassName(qualifiedName, getResolveScope());
        myRefs.add(ref);
    }

    @Override
    public void addReference(PsiClassType type) {
        final PsiClass resolved = type.resolve();
        if (resolved == null) return;

        final PsiJavaCodeReferenceElement ref = myFactory.createReferenceElementByType(type);
        myRefs.add(ref);
    }

    @NotNull
    @Override
    public PsiJavaCodeReferenceElement[] getReferenceElements() {
        if (myCachedRefs == null) {
            if (myRefs.isEmpty()) {
                myCachedRefs = PsiJavaCodeReferenceElement.EMPTY_ARRAY;
            }
            else {
                myCachedRefs = myRefs.toArray(PsiJavaCodeReferenceElement.EMPTY_ARRAY);
            }
        }
        return myCachedRefs;
    }

    @NotNull
    @Override
    public PsiClassType[] getReferencedTypes() {
        if (myCachedTypes == null) {
            if (myRefs.isEmpty()) {
                myCachedTypes = PsiClassType.EMPTY_ARRAY;
            }
            else {
                final int size = myRefs.size();
                myCachedTypes = new PsiClassType[size];
                for (int i = 0; i < size; i++) {
                    myCachedTypes[i] = myFactory.createType(myRefs.get(i));
                }
            }
        }

        return myCachedTypes;
    }

    @Override
    public Role getRole() {
        return myRole;
    }
}
