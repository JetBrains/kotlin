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

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.JetTypeConstraint
import org.jetbrains.kotlin.psi.stubs.KotlinTypeConstraintStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import com.intellij.psi.PsiElement

public class KotlinTypeConstraintStubImpl(
        parent: StubElement<out PsiElement>?,
        private val isDefaultObjectConstraint: Boolean
) : KotlinStubBaseImpl<JetTypeConstraint>(parent, JetStubElementTypes.TYPE_CONSTRAINT), KotlinTypeConstraintStub {

    override fun isDefaultObjectConstraint() = isDefaultObjectConstraint
}
