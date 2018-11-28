/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.tasks

import org.jetbrains.kotlin.spec.TestArea
import org.jetbrains.kotlin.spec.TestType
import org.jetbrains.kotlin.spec.generators.templates.Feature
import org.jetbrains.kotlin.spec.generators.templates.generationLinkedSpecTestDataConfig
import org.jetbrains.kotlin.spec.generators.templates.generationSpecTestDataConfigGroup

fun main(args: Array<String>) {
    generationSpecTestDataConfigGroup(regenerateTests = true) {
        generationLinkedSpecTestDataConfig {
            testArea = TestArea.PSI
            testType = TestType.NEGATIVE
            sections = listOf("constant-literals", "boolean-literals")
            paragraphNumber = 1
            sentenceNumber = 2
            sentence = "These are strong keywords which cannot be used as identifiers unless escaped."
            testDescription = "The use of Boolean literals as the identifier (without backtick) in the %s."
            firstFeature = Feature.IDENTIFIERS
            secondFeature = Feature.BOOLEAN_LITERALS
        }
        generationLinkedSpecTestDataConfig {
            testArea = TestArea.PSI
            testType = TestType.POSITIVE
            sections = listOf("constant-literals", "boolean-literals")
            paragraphNumber = 1
            sentenceNumber = 2
            sentence = "These are strong keywords which cannot be used as identifiers unless escaped."
            testDescription = "The use of Boolean literals as the identifier (with backtick) in the %s."
            firstFeature = Feature.IDENTIFIERS
            secondFeature = Feature.BOOLEAN_LITERALS_IN_BACKTICKS
        }
        generationLinkedSpecTestDataConfig {
            testArea = TestArea.CODEGEN_BOX
            testType = TestType.POSITIVE
            sections = listOf("constant-literals", "boolean-literals")
            paragraphNumber = 1
            sentenceNumber = 2
            sentence = "These are strong keywords which cannot be used as identifiers unless escaped."
            testDescription = "The use of Boolean literals as the identifier (with backtick) in the %s."
            firstFeature = Feature.IDENTIFIERS
            secondFeature = Feature.BOOLEAN_LITERALS_IN_BACKTICKS
        }
    }
}
