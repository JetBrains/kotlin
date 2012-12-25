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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.AbstractLightClass;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.JetLanguage;

public class KotlinLightClass extends AbstractLightClass implements JetJavaMirrorMarker {

    private final FqName fqName;
    private final JetClassOrObject jetClassOrObject;
    private final CachedValue<PsiClass> delegate;

    protected KotlinLightClass(@NotNull PsiManager manager, @NotNull FqName name, @NotNull JetClassOrObject object) {
        super(manager, JetLanguage.INSTANCE);
        this.fqName = name;
        this.jetClassOrObject = object;
        KotlinLightClassProvider stubProvider = KotlinLightClassProvider.createForDeclaredClass(getProject(), fqName, jetClassOrObject);
        this.delegate = CachedValuesManager.getManager(getProject()).createCachedValue(stubProvider);
    }

    @NotNull
    @Override
    public PsiClass getDelegate() {
        return delegate.getValue();
    }

    @NotNull
    @Override
    public PsiElement copy() {
        return new KotlinLightClass(getManager(), fqName, jetClassOrObject);
    }
}
