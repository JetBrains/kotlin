/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.generators.templates

import org.jetbrains.kotlin.spec.SpecTestLinkedType
import org.jetbrains.kotlin.spec.TestArea
import org.jetbrains.kotlin.spec.TestType

enum class SubstitutionTag(val passType: SubstitutionPassType = SubstitutionPassType.FIRST) {
    DIRECTIVES,

    // Test meta info tags
    TEST_TYPE,
    SECTIONS,
    CATEGORIES,
    PARAGRAPH_NUMBER,
    SENTENCE_NUMBER,
    SENTENCE,
    TEST_NUMBER,
    TEST_DESCRIPTION,

    // Test data tags
    ELEMENT,
    ELEMENT_VALIDATION(SubstitutionPassType.SECOND),
    CLASS_OF_FILE
}

typealias TemplatesIterator = Iterator<Map.Entry<String, String>>

abstract class GenerationSpecTestDataConfig {
    private val repeatableElements = mutableMapOf<Int, String>()

    lateinit var testType: TestType
    lateinit var testDescription: String
    lateinit var firstFeature: Feature
    lateinit var secondFeature: Feature
    lateinit var testArea: TestArea

    protected val baseSubstitutions = mapOf<SubstitutionTag, (SubstitutionRule) -> String>(
        SubstitutionTag.TEST_TYPE to { _ -> testType.toString() },
        SubstitutionTag.TEST_NUMBER to { rule -> rule.testNumber.toString() },
        SubstitutionTag.TEST_DESCRIPTION to { rule -> testDescription.format(rule.filename) },
        SubstitutionTag.ELEMENT to { rule ->
            val isRepeatableVar = rule.varNumber != null

            when {
                isRepeatableVar && repeatableElements.contains(rule.varNumber) ->
                    repeatableElements[rule.varNumber]!!
                else -> {
                    val element = secondFeature.config.getNextWithRepeat()
                    if (isRepeatableVar)
                        repeatableElements[rule.varNumber!!] = element
                    element
                }
            }
        },
        SubstitutionTag.ELEMENT_VALIDATION to { rule ->
            val validationFunction = secondFeature.config.validationTransformer
            val element = repeatableElements[rule.varNumber]!!

            when (validationFunction) {
                null -> element
                else -> templateValidationTransformers[validationFunction]!!(element)
            }
        }
    )

    private fun buildTemplatesIterator(originalIterator: TemplatesIterator) =
        object : TemplatesIterator {
            override fun next() = run {
                repeatableElements.clear()
                secondFeature.config.resetTemplatesIterator()
                originalIterator.next()
            }

            override fun hasNext() = originalIterator.hasNext()
        }

    fun getLayoutPath() = "${testArea.testDataPath}/templates/_layout/$layoutFilename"

    fun prepareAndGetFirstFeatureTemplates(): TemplatesIterator {
        secondFeature.config.testArea = testArea
        firstFeature.config.testArea = testArea

        return firstFeature.config.run {
            resetTemplatesIterator()
            buildTemplatesIterator(currentTemplatesIterator.value)
        }
    }

    abstract val layoutFilename: String
    abstract val substitutions: MutableMap<SubstitutionTag, (SubstitutionRule) -> String>
    abstract fun getTestsPartPath(): String
}

class GenerationLinkedSpecTestDataConfig : GenerationSpecTestDataConfig() {
    var paragraphNumber: Int = 0
    var sentenceNumber: Int = 0
    lateinit var sentence: String
    lateinit var sections: List<String>

    override val layoutFilename = "linkedTestsLayout.kt"
    override val substitutions = mutableMapOf<SubstitutionTag, (SubstitutionRule) -> String>(
        SubstitutionTag.SECTIONS to { _ -> sections.joinToString(", ") },
        SubstitutionTag.PARAGRAPH_NUMBER to { _ -> paragraphNumber.toString() },
        SubstitutionTag.SENTENCE_NUMBER to { _ -> sentenceNumber.toString() },
        SubstitutionTag.SENTENCE to { _ -> sentence },
        SubstitutionTag.CLASS_OF_FILE to { rule -> "_${sentenceNumber}_${rule.testNumber}Kt" }
    ).apply { putAll(baseSubstitutions) }

    override fun getTestsPartPath() =
        "${testArea.testDataPath}/${SpecTestLinkedType.LINKED.testDataPath}/${sections.joinToString("/")}/p-$paragraphNumber/${testType.type}/$sentenceNumber."
}

class GenerationNotLinkedSpecTestDataConfig : GenerationSpecTestDataConfig() {
    lateinit var categories: List<String>

    override val layoutFilename = "notLinkedTestsLayout.kt"
    override val substitutions = mutableMapOf<SubstitutionTag, (SubstitutionRule) -> String>(
        SubstitutionTag.CATEGORIES to { _ -> categories.joinToString(", ") },
        SubstitutionTag.CLASS_OF_FILE to { rule -> "_${rule.testNumber}Kt" }
    ).apply { putAll(baseSubstitutions) }

    override fun getTestsPartPath() =
        "${testArea.testDataPath}/${SpecTestLinkedType.NOT_LINKED.testDataPath}/${categories.joinToString("/")}/${testType.type}/"
}