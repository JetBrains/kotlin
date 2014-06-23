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

package org.jetbrains.jet.cfg

import org.jetbrains.jet.lang.cfg.pseudocode.PseudoValue
import org.jetbrains.jet.lang.cfg.pseudocode.PseudocodeImpl
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid
import org.jetbrains.jet.lang.resolve.BindingContext
import java.util.*
import org.jetbrains.jet.lang.cfg.pseudocode.collectValueUsages
import org.jetbrains.jet.lang.cfg.pseudocode.TypePredicate
import org.jetbrains.jet.lang.cfg.pseudocode.getExpectedTypePredicate

public abstract class AbstractPseudoValueTest : AbstractPseudocodeTest() {
    override fun dumpInstructions(pseudocode: PseudocodeImpl, out: StringBuilder, bindingContext: BindingContext) {
        val valueUsageMap = pseudocode.collectValueUsages()
        val expectedTypePredicateMap = HashMap<PseudoValue, TypePredicate>()

        fun getElementToValueMap(pseudocode: PseudocodeImpl): Map<JetElement, PseudoValue> {
            val elementToValues = LinkedHashMap<JetElement, PseudoValue>()
            pseudocode.getCorrespondingElement().accept(object : JetTreeVisitorVoid() {
                override fun visitJetElement(element: JetElement) {
                    super.visitJetElement(element)

                    val value = pseudocode.getElementValue(element)
                    if (value != null) {
                        elementToValues.put(element, value)
                    }
                }
            })
            return elementToValues
        }

        fun maxLength(strings: Iterable<String>): Int = strings.map { it.length }.max() ?: 0

        fun elementText(element: JetElement): String = element.getText()!!.replaceAll("\\s+", " ")

        fun valueDecl(value: PseudoValue): String {
            val typePredicate = expectedTypePredicateMap.getOrPut(value) { getExpectedTypePredicate(value, valueUsageMap, bindingContext) }
            return "${value.debugName}: $typePredicate"
        }

        fun valueDescription(element: JetElement, value: PseudoValue): String {
            return if (value.element != element) "COPY" else "NEW${value.createdAt.inputValues.makeString(", ", "(", ")")}"
        }

        val elementToValues = getElementToValueMap(pseudocode)
        if (elementToValues.isEmpty()) return

        val elementColumnWidth = elementToValues.keySet().map { elementText(it).length() }.max()!!
        val valueColumnWidth = elementToValues.values().map { valueDecl(it).length() }.max()!!
        val valueDescColumnWidth = elementToValues.entrySet().map { valueDescription(it.key, it.value).length }.max()!!

        for ((element, value) in elementToValues.entrySet()) {
            out
                    .append("%1$-${elementColumnWidth}s".format(elementText(element)))
                    .append("   ")
                    .append("%1$-${valueColumnWidth}s".format(valueDecl(value)))
                    .append("   ")
                    .append("%1$-${valueDescColumnWidth}s".format(valueDescription(element, value)))
                    .append("\n")
        }
    }

    override fun getDataFileExtension(): String? = "values"
}
