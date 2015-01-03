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

package org.jetbrains.jet.asJava

import com.intellij.psi.PsiManager
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.JetEnumEntry
import com.intellij.psi.PsiEnumConstantInitializer
import com.intellij.psi.PsiEnumConstant

public class KotlinLightClassForEnumEntry(
        psiManager: PsiManager,
        fqName: FqName,
        enumEntry: JetEnumEntry,
        private val enumConstant: PsiEnumConstant
): KotlinLightClassForAnonymousDeclaration(psiManager, fqName, enumEntry), PsiEnumConstantInitializer {
    override fun getEnumConstant(): PsiEnumConstant = enumConstant
}