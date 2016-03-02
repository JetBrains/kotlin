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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.light.LightIdentifier
import org.jetbrains.kotlin.psi.KtNamedDeclaration

class KtLightIdentifier(
        private val lightOwner: PsiNameIdentifierOwner,
        private val ktDeclaration: KtNamedDeclaration?
) : LightIdentifier(lightOwner.manager, ktDeclaration?.nameIdentifier?.text ?: "") {
    override fun getParent() = lightOwner
    override fun getTextRange() = ktDeclaration?.nameIdentifier?.textRange ?: TextRange.EMPTY_RANGE
}