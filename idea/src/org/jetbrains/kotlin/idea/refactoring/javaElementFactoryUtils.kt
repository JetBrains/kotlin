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

import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.PsiReferenceList.Role

public fun PsiElementFactory.createReferenceListWithRole(
        references: Array<PsiJavaCodeReferenceElement>,
        role: Role
): PsiReferenceList? {
    val refsText = references.map { it.getCanonicalText() }
    val refListText = if (refsText.isNotEmpty()) refsText.joinToString() else return null
    return when (role) {
        Role.THROWS_LIST -> createMethodFromText("void foo() throws $refListText {}", null).getThrowsList()
        Role.EXTENDS_LIST -> createClassFromText("class Foo extends $refListText {}", null).getInnerClasses()[0].getExtendsList()
        Role.IMPLEMENTS_LIST -> createClassFromText("class Foo implements $refListText {}", null).getInnerClasses()[0].getImplementsList()
        Role.EXTENDS_BOUNDS_LIST -> createTypeParameterFromText("T extends $refListText", null).getExtendsList()
    }
}
