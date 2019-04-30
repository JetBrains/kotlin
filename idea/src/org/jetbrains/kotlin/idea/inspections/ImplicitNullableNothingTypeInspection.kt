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

package org.jetbrains.kotlin.idea.inspections

import org.jetbrains.kotlin.idea.core.getModalityFromDescriptor
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.types.typeUtil.isNullableNothing

class ImplicitNullableNothingTypeInspection : IntentionBasedInspection<KtCallableDeclaration>(
        intention = SpecifyTypeExplicitlyIntention::class,
        additionalChecker = { declaration ->
            SpecifyTypeExplicitlyIntention.getTypeForDeclaration(declaration).isNullableNothing() &&
            (declaration.getModalityFromDescriptor() == KtTokens.OPEN_KEYWORD ||
             declaration is KtProperty && declaration.isVar)
        },
        problemText = "Implicit `Nothing?` type"
) {
    override fun inspectionTarget(element: KtCallableDeclaration) = element.nameIdentifier
}
