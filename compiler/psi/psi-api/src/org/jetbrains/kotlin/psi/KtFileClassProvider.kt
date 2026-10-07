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
 * A service that computes the Java classes of a [KtFile]: the [PsiClass]es for the JVM classes that correspond to the file, such as its
 * file facade class and top-level classes.
 *
 * It backs [KtFile.getClasses]. The implementation is supplied by the platform, since it depends on the analysis environment: for example,
 * the classes may be light classes built from the Kotlin declarations or, for a compiled file, Java classes built from its class file.
 */
interface KtFileClassProvider {
    /**
     * Returns the Java classes of [file], or an empty array if the platform provides none for it.
     */
    fun getFileClasses(file: KtFile): Array<PsiClass>
}
