/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.generators.templates

import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.TESTDATA_PATH
import java.io.File
import java.util.regex.Pattern

enum class SubstitutionPassType { FIRST, SECOND }

data class SubstitutionRule(
    val tag: SubstitutionTag,
    val filename: String,
    val testNumber: Int,
    val varNumber: Int? = null
)

class FeatureInteractionTestDataGenerator(private val configuration: GenerationSpecTestDataConfig) {
    companion object {
        const val TEMPLATES_PATH = "templates"

        private const val PARAMETER_REGEXP = """(?:".*?"|.*?)"""

        private fun getVariablePattern(varRegex: String = ".*?", afterContent: String = "") =
            Pattern.compile("""<!(?<varName>$varRegex)(?:\((?<parameters>$PARAMETER_REGEXP(?:,\s*$PARAMETER_REGEXP)*)\))?!>$afterContent""")

        private fun String.extractDirectives(): Pair<String, String> {
            val matcher = getVariablePattern(
                varRegex = SubstitutionTag.DIRECTIVES.name,
                afterContent = System.lineSeparator().repeat(2)
            ).matcher(this)

            if (!matcher.find())
                return Pair("", this)

            val parameters = parseParameters(matcher.group("parameters"))
            val directives = parameters.joinToString { "// $it${System.lineSeparator()}" } + System.lineSeparator()
            val template = StringBuffer(this.length).let {
                matcher.appendReplacement(it, "").appendTail(it).toString()
            }

            return Pair(directives, template)
        }

        private fun parseParameters(rawParameters: String) =
            rawParameters.split(Regex(""",\s*""")).map { it.trim('"') }
    }

    private fun String.substitute(
        filename: String,
        testNumber: Int,
        passType: SubstitutionPassType = SubstitutionPassType.FIRST
    ): String {
        val buffer = StringBuffer(this.length)
        val matcher = getVariablePattern().matcher(this)
        while (matcher.find()) {
            val varName = matcher.group("varName")
            val rawParameters = matcher.group("parameters")
            val varNumber = if (rawParameters != null) parseParameters(rawParameters)[0].toInt() else null
            val tag = SubstitutionTag.valueOf(varName)

            if (tag.passType != passType)
                continue

            matcher.appendReplacement(
                buffer,
                configuration.substitutions[tag]?.invoke(
                    SubstitutionRule(tag, filename, testNumber, varNumber)
                )
            )
        }

        return matcher.appendTail(buffer).toString()
    }

    fun generate() {
        var testNumber = 1
        val testsPartPath = "$TESTDATA_PATH/${configuration.getTestsPartPath()}"
        val layoutTemplate = File("$TESTDATA_PATH/${configuration.getLayoutPath()}").readText()

        File(testsPartPath).parentFile.mkdirs()

        for ((filename, template) in configuration.prepareAndGetFirstFeatureTemplates()) {
            val (directives, templateWithoutDirectives) = template.extractDirectives()
            val code = templateWithoutDirectives
                .substitute(filename, testNumber, SubstitutionPassType.FIRST)
                .substitute(filename, testNumber, SubstitutionPassType.SECOND)
            val layout = layoutTemplate.substitute(filename, testNumber)
            val testFilePath = "$testsPartPath$testNumber.kt"

            File(testFilePath).writeText(directives + layout + System.lineSeparator().repeat(2) + code)
            testNumber++
        }
    }
}
