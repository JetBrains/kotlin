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

package org.jetbrains.kotlin.psi

import com.intellij.psi.PsiClass

/**
 * A service that computes the Java light classes exposed by a [KtFile] (its file facade class and top-level class
 * declarations).
 *
 * It backs [KtFile.getClasses]; the concrete implementation is supplied by the surrounding platform, since light-class
 * generation depends on the analysis environment.
 */
interface KtFileClassProvider {
    /**
     * Returns the Java light classes contributed by the given file, or an empty array if none are available.
     */
    fun getFileClasses(file: KtFile): Array<PsiClass>
}
