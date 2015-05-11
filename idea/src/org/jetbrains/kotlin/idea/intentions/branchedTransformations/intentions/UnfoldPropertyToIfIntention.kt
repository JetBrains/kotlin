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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedUnfoldingUtils
import org.jetbrains.kotlin.psi.JetIfExpression
import org.jetbrains.kotlin.psi.JetProperty

public class UnfoldPropertyToIfIntention : JetSelfTargetingOffsetIndependentIntention<JetProperty>(javaClass(), "Replace property initializer with 'if' expression") {
    override fun isApplicableTo(element: JetProperty): Boolean {
        if (!element.isLocal()) return false
        return element.getInitializer() is JetIfExpression
    }

    override fun applyTo(element: JetProperty, editor: Editor) {
        BranchedUnfoldingUtils.unfoldPropertyToIf(element, editor)
    }
}