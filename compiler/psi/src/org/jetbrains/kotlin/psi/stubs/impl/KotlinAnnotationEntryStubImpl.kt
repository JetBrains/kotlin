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
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationEntryStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.name.Name

class KotlinAnnotationEntryStubImpl(
    parent: StubElement<out PsiElement>?,
    private val shortName: StringRef?,
    private val hasValueArguments: Boolean,
    val valueArguments: Map<Name, ConstantValue<*>>?
) : KotlinStubBaseImpl<KtAnnotationEntry>(parent, KtStubElementTypes.ANNOTATION_ENTRY), KotlinAnnotationEntryStub {

    override fun getShortName() = shortName?.string

    override fun hasValueArguments() = hasValueArguments
}
