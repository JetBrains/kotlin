/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.generators.templates

import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.SPEC_TESTDATA_PATH
import org.jetbrains.kotlin.spec.utils.TestArea
import java.io.File

enum class FeatureTemplatesType {
    AS_FILE,
    AS_STRING
}

class FeatureTemplatesConfig(
    private val featureTemplatesType: FeatureTemplatesType,
    private val templatesPath: String? = null,
    private val templates: Map<String, String>? = null,
    val validationTransformer: TemplateValidationTransformerType? = null
) {
    lateinit var testArea: TestArea
    var currentTemplatesIterator = getTemplatesIterator()

    private fun getTemplatesPath(testArea: TestArea) = buildString {
        append(SPEC_TESTDATA_PATH)
        append("/${testArea.testDataPath}")
        append("/${FeatureInteractionTestDataGenerator.TEMPLATES_PATH}")
        append("/$templatesPath")
    }

    private fun getTemplateFiles(testArea: TestArea) =
        File(getTemplatesPath(testArea)).walkTopDown().filter { it.extension == "kt" }
            .associate { Pair(it.nameWithoutExtension, it.readText()) }

    private fun getTemplates(testArea: TestArea) = lazy {
        when (featureTemplatesType) {
            FeatureTemplatesType.AS_STRING -> templates!!
            FeatureTemplatesType.AS_FILE -> getTemplateFiles(testArea)
        }
    }

    private fun getTemplatesIterator() = lazy { getTemplates(testArea).value.iterator() }

    fun resetTemplatesIterator() {
        currentTemplatesIterator = getTemplatesIterator()
    }

    fun getNextWithRepeat() = let {
        takeUnless { it.currentTemplatesIterator.value.hasNext() }?.resetTemplatesIterator()
        currentTemplatesIterator.value.next().value
    }
}