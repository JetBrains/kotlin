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

package org.jetbrains.jet.asJava

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import org.jetbrains.jet.lang.psi.JetDeclaration
import com.intellij.psi.PsiClass
import org.jetbrains.jet.asJava.KotlinLightMethodForTraitFakeOverride
import com.intellij.psi.PsiElement


public class KotlinLightMethodForTraitFakeOverride(manager: PsiManager,
                                                   override val delegate: PsiMethod,
                                                   override val origin: JetDeclaration,
                                                   containingClass: PsiClass) :
        KotlinLightMethodForDeclaration(manager, delegate, origin, containingClass) {

    override fun copy(): PsiElement {
        return KotlinLightMethodForTraitFakeOverride(getManager()!!, delegate, origin.copy() as JetDeclaration, getContainingClass()!!)
    }
}