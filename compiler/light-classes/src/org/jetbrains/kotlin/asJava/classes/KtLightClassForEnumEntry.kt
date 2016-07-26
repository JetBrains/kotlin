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

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiEnumConstantInitializer
import org.jetbrains.kotlin.psi.KtEnumEntry

internal class KtLightClassForEnumEntry(
        enumEntry: KtEnumEntry,
        private val enumConstant: PsiEnumConstant
): KtLightClassForAnonymousDeclaration(enumEntry), PsiEnumConstantInitializer {
    override fun getEnumConstant(): PsiEnumConstant = enumConstant
    override fun copy() = KtLightClassForEnumEntry(classOrObject as KtEnumEntry, enumConstant)

    override fun getParent() = enumConstant
}
