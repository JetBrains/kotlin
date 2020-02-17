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
package org.jetbrains.kotlin.idea.refactoring

import com.intellij.CommonBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.kotlin.idea.core.util.KotlinBundleBase
import java.util.*

object KotlinRefactoringBundle : KotlinBundleBase() {
    @NonNls
    private const val BUNDLE = "org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle"

    override fun createBundle(): ResourceBundle = ResourceBundle.getBundle(BUNDLE)

    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?): String {
        return CommonBundle.message(bundle, key, *params)
    }
}