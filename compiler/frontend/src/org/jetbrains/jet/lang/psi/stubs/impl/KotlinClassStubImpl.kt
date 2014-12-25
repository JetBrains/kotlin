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
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.stubs.KotlinClassStub
import org.jetbrains.jet.lang.psi.stubs.elements.JetClassElementType
import org.jetbrains.jet.lang.resolve.name.FqName

import java.util.ArrayList
import com.intellij.psi.PsiElement

public class KotlinClassStubImpl(
        type: JetClassElementType,
        parent: StubElement<out PsiElement>?,
        private val qualifiedName: StringRef?,
        private val name: StringRef?,
        private val superNames: Array<StringRef>,
        private val isTrait: Boolean,
        private val isEnumEntry: Boolean,
        private val isLocal: Boolean,
        private val isTopLevel: Boolean
) : KotlinStubBaseImpl<JetClass>(parent, type), KotlinClassStub {

    override fun getFqName(): FqName? {
        val stringRef = StringRef.toString(qualifiedName)
        if (stringRef == null) {
            return null
        }
        return FqName(stringRef)
    }

    override fun isTrait() = isTrait
    override fun isEnumEntry() = isEnumEntry
    override fun isLocal() = isLocal
    override fun getName() = StringRef.toString(name)

    override fun getSuperNames(): List<String> {
        val result = ArrayList<String>()
        for (ref in superNames) {
            result.add(ref.toString())
        }
        return result
    }

    override fun isTopLevel() = isTopLevel
}
