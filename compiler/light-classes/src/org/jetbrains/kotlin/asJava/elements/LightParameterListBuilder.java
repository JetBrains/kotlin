/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava.elements;

import com.intellij.lang.Language;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// Copy of com.intellij.psi.impl.light.LightParameterListBuilder
public class LightParameterListBuilder extends LightElement implements PsiParameterList {
    private final List<PsiParameter> myParameters = new ArrayList<PsiParameter>();
    private final KtLightMethod parent;
    private PsiParameter[] myCachedParameters;

    public LightParameterListBuilder(PsiManager manager, Language language, KtLightMethod parent) {
        super(manager, language);
        this.parent = parent;
    }

    public void addParameter(PsiParameter parameter) {
        myParameters.add(parameter);
        myCachedParameters = null;
    }

    @Override
    public KtLightMethod getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return "Light parameter list";
    }

    @NotNull
    @Override
    public PsiParameter[] getParameters() {
        if (myCachedParameters == null) {
            if (myParameters.isEmpty()) {
                myCachedParameters = PsiParameter.EMPTY_ARRAY;
            }
            else {
                myCachedParameters = myParameters.toArray(new PsiParameter[myParameters.size()]);
            }
        }

        return myCachedParameters;
    }

    @Override
    public int getParameterIndex(PsiParameter parameter) {
        return myParameters.indexOf(parameter);
    }

    @Override
    public int getParametersCount() {
        return myParameters.size();
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor) visitor).visitParameterList(this);
        }
    }
}
