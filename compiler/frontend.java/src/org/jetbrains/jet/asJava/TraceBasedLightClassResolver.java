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

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.KotlinLightClassResolver;

public class TraceBasedLightClassResolver implements KotlinLightClassResolver {
    private final BindingContext bindingContext;

    public TraceBasedLightClassResolver(@NotNull BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    @Nullable
    @Override
    public ClassDescriptor resolveLightClass(@NotNull PsiClass kotlinLightClass) {
        assert kotlinLightClass instanceof KotlinLightClass
                : "Wrong light class: " + kotlinLightClass + " " + kotlinLightClass.getClass();
        JetClassOrObject sourceElement = ((KotlinLightClass) kotlinLightClass).getSourceElement();
        if (sourceElement == null) {
            return null; // package class, invisible from Kotlin
        }
        ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, sourceElement);
        assert classDescriptor != null : "No class descriptor found for" +
                                         "\nlight class " + kotlinLightClass.getQualifiedName() +
                                         "\nkotlin psi element " + sourceElement +
                                         "\nfile:" + sourceElement.getContainingFile() +
                                         "\n" + sourceElement.getContainingFile().getText();
        return classDescriptor;
    }
}
