/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.arguments.InternalArgumentParser.Companion.INTERNAL_ARGUMENT_PREFIX
import org.jetbrains.kotlin.config.LanguageFeature

/**
 * Arguments that can drastically change compiler behavior,
 * breaking stability/compatibility.
 *
 * Internal arguments are split into 'families', each family
 * with its own set of arguments, settings and parsing rules
 *
 * Internal arguments start with '-XX' prefix, followed by
 * family name. Everything after that is handled by the corresponding
 * parser of that particular family.
 */
interface InternalArgumentParser<A : InternalArgument> {
    // Should be fast
    fun canParse(arg: String): Boolean

    fun parseInternalArgument(arg: String, errors: ArgumentParseErrors): A?

    companion object {
        internal const val INTERNAL_ARGUMENT_PREFIX = "-XX"

        internal val PARSERS: List<InternalArgumentParser<*>> = listOf(
            LanguageSettingsParser()
        )
    }
}

abstract class AbstractInternalArgumentParser<A : InternalArgument>(familyName: String) : InternalArgumentParser<A> {
    private val wholePrefix: String = INTERNAL_ARGUMENT_PREFIX + familyName

    override fun canParse(arg: String): Boolean = arg.startsWith(wholePrefix)

    override fun parseInternalArgument(arg: String, errors: ArgumentParseErrors): A? {
        if (!arg.startsWith(wholePrefix)) return null

        return parseTail(arg.removePrefix(wholePrefix), arg, errors)
    }

    abstract fun parseTail(tail: String, wholeArgument: String, errors: ArgumentParseErrors): A?
}


// Arguments of form '-XXLanguage:+LanguageFeature' or '-XXLanguage:-LanguageFeature', which enable or disable corresponding LanguageFeature.
class LanguageSettingsParser : AbstractInternalArgumentParser<ManualLanguageFeatureSetting>("Language") {

    // Expected tail form: ':(+|-)<language feature name>'
    override fun parseTail(tail: String, wholeArgument: String, errors: ArgumentParseErrors): ManualLanguageFeatureSetting? {
        fun reportAndReturnNull(message: String): Nothing? {
            errors.internalArgumentsParsingProblems += message
            return null
        }

        val colon = tail.getOrNull(0) ?: return reportAndReturnNull("Incorrect internal argument syntax, missing colon: $wholeArgument")

        val modificator = tail.getOrNull(1)
        val languageFeatureState = when (modificator) {
            '+' -> LanguageFeature.State.ENABLED

            '-' -> LanguageFeature.State.DISABLED

            else -> return reportAndReturnNull("Incorrect internal argument syntax, missing modificator: $wholeArgument")
        }

        val languageFeatureName = tail.substring(2)
        if (languageFeatureName.isEmpty()) return reportAndReturnNull("Empty language feature name for internal argument '$wholeArgument'")

        val languageFeature = LanguageFeature.fromString(languageFeatureName)
            ?: return reportAndReturnNull("Unknown language feature '$languageFeatureName' in passed internal argument '$wholeArgument'")

        return ManualLanguageFeatureSetting(languageFeature, languageFeatureState, wholeArgument)
    }
}

interface InternalArgument {
    val stringRepresentation: String
}

data class ManualLanguageFeatureSetting(
    val languageFeature: LanguageFeature,
    val state: LanguageFeature.State,
    override val stringRepresentation: String
) : InternalArgument