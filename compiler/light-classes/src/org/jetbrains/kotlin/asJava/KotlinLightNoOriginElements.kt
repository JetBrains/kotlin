/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightField
import com.intellij.psi.impl.light.LightMethod


public class KotlinNoOriginLightMethod(manager: PsiManager, method: PsiMethod, containingClass: PsiClass) :
        LightMethod(manager, method, containingClass) {

    override fun toString() = "KotlinNoOriginLightMethod:${getName()}" + if (isConstructor()) " ctor" else ""

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitMethod(this)
        }
        else {
            visitor.visitElement(this)
        }
    }
}

public class KotlinNoOriginLightField(manager: PsiManager, field: PsiField, containingClass: PsiClass) :
        LightField(manager, field, containingClass) {

    override fun toString() = "KotlinNoOriginLightField:${getName()}"
}