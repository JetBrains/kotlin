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

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.psi.PsiElement
import java.util.*


class ElementAndTextList() {
    private val elementsAndTexts = ArrayList<Any>()

    constructor(elements: List<Any>) : this() {
        elementsAndTexts.addAll(elements.filter { it is PsiElement || it is String })
    }

    fun add(a: String) = elementsAndTexts.add(a)

    fun add(a: PsiElement) = elementsAndTexts.add(a)

    operator fun plusAssign(other: String) = plusAssign(other as Any)

    operator fun plusAssign(other: PsiElement) = plusAssign(other as Any)

    private fun plusAssign(a: Any) {
        elementsAndTexts.add(a)
    }

    operator fun plusAssign(other: Collection<PsiElement>): Unit {
        elementsAndTexts.addAll(other)
    }

    operator fun plus(other: ElementAndTextList): ElementAndTextList {
        val newList = ElementAndTextList()
        newList.elementsAndTexts.addAll(this.elementsAndTexts)
        newList.elementsAndTexts.addAll(other.elementsAndTexts)
        return newList
    }

    fun toList(): List<Any> = elementsAndTexts.toList()

    fun process(processor: ElementsAndTextsProcessor) {
        elementsAndTexts.forEach {
            when (it) {
                is PsiElement -> processor.processElement(it)
                is String -> processor.processText(it)
            }
        }
    }
}

interface ElementsAndTextsProcessor {
    fun processElement(element: PsiElement)
    fun processText(string: String)
}