/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.stubs.KotlinPropertyStub
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes
import org.jetbrains.jet.lang.resolve.name.FqName
import com.intellij.psi.PsiElement

public class KotlinPropertyStubImpl(
        parent: StubElement<out PsiElement>?,
        private val name: StringRef?,
        private val isVar: Boolean,
        private val isTopLevel: Boolean,
        private val hasDelegate: Boolean,
        private val hasDelegateExpression: Boolean,
        private val hasInitializer: Boolean,
        private val hasReceiverTypeRef: Boolean,
        private val hasReturnTypeRef: Boolean,
        private val isProbablyNothingType: Boolean,
        private val fqName: FqName?
) : KotlinStubBaseImpl<JetProperty>(parent, JetStubElementTypes.PROPERTY), KotlinPropertyStub {

    {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level properties")
        }
        if (hasDelegateExpression && !hasDelegate) {
            throw IllegalArgumentException("Can't have delegate expression without delegate")
        }
    }

    override fun getFqName() = fqName
    override fun isVar() = isVar
    override fun isTopLevel() = isTopLevel
    override fun hasDelegate() = hasDelegate
    override fun hasDelegateExpression() = hasDelegateExpression
    override fun hasInitializer() = hasInitializer
    override fun hasReceiverTypeRef() = hasReceiverTypeRef
    override fun hasReturnTypeRef() = hasReturnTypeRef
    override fun getName() = StringRef.toString(name)
    override fun isProbablyNothingType() = isProbablyNothingType
}
