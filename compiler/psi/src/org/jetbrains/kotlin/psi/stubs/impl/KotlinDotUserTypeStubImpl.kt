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

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtDotUserType
import org.jetbrains.kotlin.psi.stubs.KotlinDotUserTypeStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

/**
 * @param upperBound If [upperBound] exists, this [KotlinDotUserTypeStubImpl] is a *flexible type* with itself as the lower bound and
 *  [upperBound] as the upper bound.
 * @param abbreviatedType The type alias application from which this type was originally expanded. It can be used to render or navigate to
 *  the original type alias instead of the expanded type.
 */
class KotlinDotUserTypeStubImpl(
    parent: StubElement<out PsiElement>?,
    val upperBound: KotlinTypeBean? = null,
    val abbreviatedType: KotlinClassTypeBean? = null,
) : KotlinStubBaseImpl<KtDotUserType>(parent, KtStubElementTypes.DOT_USER_TYPE), KotlinDotUserTypeStub