/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ConvertClassToSealedClassIntention : SelfTargetingRangeIntention<KtClass>(KtClass::class.java, "Convert to sealed class") {

    override fun applicabilityRange(element: KtClass): TextRange? {
        if (element.modifierList == null) return null
        if (!element.hasModifier(OPEN_KEYWORD) && !element.hasModifier(ABSTRACT_KEYWORD)) return null

        val constructors = listOfNotNull(element.primaryConstructor) + element.secondaryConstructors
        if (constructors.isEmpty()) return null
        if (!constructors.all { it.hasModifier(PRIVATE_KEYWORD) && it.getAnnotationEntries().isEmpty() }) return null

        val nameIdentifier = element.nameIdentifier ?: return null
        return TextRange(element.startOffset, nameIdentifier.endOffset)
    }

    override fun applyTo(element: KtClass, editor: Editor?) {
        element.modifierList?.run {
            getModifier(OPEN_KEYWORD)?.delete()
            getModifier(ABSTRACT_KEYWORD)?.delete()
        }
        element.addModifier(SEALED_KEYWORD)

        element.primaryConstructor?.run {
            if (element.secondaryConstructors.isEmpty() && valueParameters.isEmpty()) {
                this.delete()
            }
            else {
                val newConstructor = this.copy() as KtPrimaryConstructor
                newConstructor.modifierList?.getModifier(PRIVATE_KEYWORD)?.delete()
                newConstructor.getConstructorKeyword()?.delete()
                this.replace(newConstructor)
            }
        }

        element.secondaryConstructors.forEach {
            it.modifierList?.getModifier(PRIVATE_KEYWORD)?.delete()
        }
    }

}
