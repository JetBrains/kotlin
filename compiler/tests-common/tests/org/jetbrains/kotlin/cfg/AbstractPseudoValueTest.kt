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

package org.jetbrains.kotlin.cfg

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeImpl
import org.jetbrains.kotlin.cfg.pseudocode.TypePredicate
import org.jetbrains.kotlin.cfg.pseudocode.getExpectedTypePredicate
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.InstructionWithValue
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.*

abstract class AbstractPseudoValueTest : AbstractPseudocodeTest() {
    override fun dumpInstructions(pseudocode: PseudocodeImpl, out: StringBuilder, bindingContext: BindingContext) {
        val expectedTypePredicateMap = HashMap<PseudoValue, TypePredicate>()

        fun getElementToValueMap(pseudocode: PseudocodeImpl): Map<KtElement, PseudoValue> {
            val elementToValues = LinkedHashMap<KtElement, PseudoValue>()
            pseudocode.correspondingElement.accept(object : KtTreeVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    super.visitKtElement(element)

                    val value = pseudocode.getElementValue(element)
                    if (value != null) {
                        elementToValues.put(element, value)
                    }
                }
            })
            return elementToValues
        }

        fun elementText(element: KtElement?): String =
                element?.text?.replace("\\s+".toRegex(), " ") ?: ""

        fun valueDecl(value: PseudoValue): String {
            val typePredicate = expectedTypePredicateMap.getOrPut(value) {
                getExpectedTypePredicate(value, bindingContext, DefaultBuiltIns.Instance)
            }
            return "${value.debugName}: $typePredicate"
        }

        fun valueDescription(element: KtElement?, value: PseudoValue): String {
            return when {
                value.element != element -> "COPY"
                else -> value.createdAt?.let { "NEW: $it" } ?: ""
            }
        }

        val elementToValues = getElementToValueMap(pseudocode)
        val unboundValues = pseudocode.instructions
                .mapNotNull { (it as? InstructionWithValue)?.outputValue }
                .filter { it.element == null }
                .sortedBy { it.debugName }
        val allValues = elementToValues.values + unboundValues
        if (allValues.isEmpty()) return

        val valueDescriptions = LinkedHashMap<Pair<PseudoValue, KtElement?>, String>()
        for (value in unboundValues) {
            valueDescriptions[value to null] = valueDescription(null, value)
        }
        for ((element, value) in elementToValues.entries) {
            valueDescriptions[value to element] = valueDescription(element, value)
        }

        val elementColumnWidth = elementToValues.keys.map { elementText(it).length }.max() ?: 1
        val valueColumnWidth = allValues.map { valueDecl(it).length }.max()!!
        val valueDescColumnWidth = valueDescriptions.values.map { it.length }.max()!!

        for ((ve, description) in valueDescriptions.entries) {
            val (value, element) = ve
            val line =
                    "%1$-${elementColumnWidth}s".format(elementText(element)) +
                     "   " +
                     "%1$-${valueColumnWidth}s".format(valueDecl(value)) +
                     "   " +
                     "%1$-${valueDescColumnWidth}s".format(description)
            out.appendln(line.trimEnd())
        }
    }

    override fun getDataFileExtension(): String? = "values"
}
