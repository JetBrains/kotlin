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

package org.jetbrains.kotlin.resolve.calls.util

import com.intellij.psi.StubBasedPsiElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration

/**
 * val lambda = fun(x: Int, _: String, `_`: Double) = 1
 *
 * This property is true only for second value parameter in the example above
 */
val KtNamedDeclaration.isSingleUnderscore: Boolean
    get() {
        // We don't want to call 'getNameIdentifier' on stubs to prevent text building
        // But it's fine because one-underscore names are prohibited for non-local declarations (only lambda parameters, local vars are allowed)
        if (this is StubBasedPsiElement<*> && this.stub != null) return false
        return nameIdentifier?.text == "_"
    }
