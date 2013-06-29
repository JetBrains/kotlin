/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.jet.lang.resolve.name.FqName;

/**
 * TODO: make more accurate wrapper
 */
public class JetLightPackage extends PsiPackageImpl {

    private final FqName fqName;
    private final GlobalSearchScope scope;

    public JetLightPackage(PsiManager manager, FqName qualifiedName, GlobalSearchScope scope) {
        super(manager, qualifiedName.asString());
        this.fqName = qualifiedName;
        this.scope = scope;
    }

    @Override
    public PsiElement copy() {
        return new JetLightPackage(getManager(), fqName, scope);
    }

    @Override
    public boolean isValid() {
        return LightClassGenerationSupport.getInstance(getProject()).packageExists(fqName, scope);
    }
}
