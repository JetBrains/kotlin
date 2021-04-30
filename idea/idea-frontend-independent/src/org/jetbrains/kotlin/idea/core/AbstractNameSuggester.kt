/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart

abstract class AbstractNameSuggester {
    fun suggestNamesByFqName(
        fqName: FqName,
        ignoreCompanion: Boolean = true,
        validator: (String) -> Boolean = { true },
        defaultName: () -> String? = { null }
    ): Collection<String> {
        val result = LinkedHashSet<String>()

        var name = ""
        fqName.asString().split('.').asReversed().forEach {
            if (ignoreCompanion && it == "Companion") return@forEach
            name = name.withPrefix(it)
            result.addName(name, validator)
        }

        if (result.isEmpty()) {
            result.addName(defaultName(), validator)
        }

        return result
    }

    /**
     * Validates name, and slightly improves it by adding number to name in case of conflicts
     * @param name to check it in scope
     * @return name or nameI, where I is number
     */
    fun suggestNameByName(name: String, validator: (String) -> Boolean): String {
        if (validator(name)) return name
        var i = 1
        while (i <= MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS && !validator(name + i)) {
            ++i
        }

        return name + i
    }


    fun suggestNamesForTypeParameters(count: Int, validator: (String) -> Boolean): List<String> {
        val result = ArrayList<String>()
        for (i in 0 until count) {
            result.add(suggestNameByMultipleNames(COMMON_TYPE_PARAMETER_NAMES, validator))
        }
        return result
    }

    fun suggestTypeAliasNameByPsi(typeElement: KtTypeElement, validator: (String) -> Boolean): String {
        fun KtTypeElement.render(): String {
            return when (this) {
                is KtNullableType -> "Nullable${innerType?.render() ?: ""}"
                is KtFunctionType -> {
                    val arguments = listOfNotNull(receiverTypeReference) + parameters.mapNotNull { it.typeReference }
                    val argText = arguments.joinToString(separator = "") { it.typeElement?.render() ?: "" }
                    val returnText = returnTypeReference?.typeElement?.render() ?: "Unit"
                    "${argText}To$returnText"
                }
                is KtUserType -> {
                    val argText = typeArguments.joinToString(separator = "") { it.typeReference?.typeElement?.render() ?: "" }
                    "$argText${referenceExpression?.text ?: ""}"
                }
                else -> text.capitalizeAsciiOnly()
            }
        }

        return suggestNameByName(typeElement.render(), validator)
    }

    /**
     * Validates name using set of variants which are tried in succession (and extended with suffixes if necessary)
     * For example, when given sequence of a, b, c possible names are tried out in the following order: a, b, c, a1, b1, c1, a2, b2, c2, ...
     * @param names to check it in scope
     * @return name or nameI, where name is one of variants and I is a number
     */
    fun suggestNameByMultipleNames(names: Collection<String>, validator: (String) -> Boolean): String {
        var i = 0
        while (true) {
            for (name in names) {
                val candidate = if (i > 0) name + i else name
                if (validator(candidate)) return candidate
            }
            i++
        }
    }

    fun getCamelNames(name: String, validator: (String) -> Boolean, startLowerCase: Boolean): List<String> {
        val result = ArrayList<String>()
        result.addCamelNames(name, validator, startLowerCase)
        return result
    }

    protected fun MutableCollection<String>.addCamelNames(name: String, validator: (String) -> Boolean, startLowerCase: Boolean = true) {
        if (name === "" || !name.unquote().isIdentifier()) return
        var s = extractIdentifiers(name)

        for (prefix in ACCESSOR_PREFIXES) {
            if (!s.startsWith(prefix)) continue

            val len = prefix.length
            if (len < s.length && Character.isUpperCase(s[len])) {
                s = s.substring(len)
                break
            }
        }

        var upperCaseLetterBefore = false
        for (i in s.indices) {
            val c = s[i]
            val upperCaseLetter = Character.isUpperCase(c)

            if (i == 0) {
                addName(if (startLowerCase) s.decapitalizeSmart() else s, validator)
            } else {
                if (upperCaseLetter && !upperCaseLetterBefore) {
                    val substring = s.substring(i)
                    addName(if (startLowerCase) substring.decapitalizeSmart() else substring, validator)
                }
            }

            upperCaseLetterBefore = upperCaseLetter
        }
    }

    private fun extractIdentifiers(s: String): String {
        return buildString {
            val lexer = KotlinLexer()
            lexer.start(s)
            while (lexer.tokenType != null) {
                if (lexer.tokenType == KtTokens.IDENTIFIER) {
                    append(lexer.tokenText)
                }
                lexer.advance()
            }
        }
    }

    protected fun MutableCollection<String>.addNamesByExpressionPSI(expression: KtExpression?, validator: (String) -> Boolean) {
        if (expression == null) return
        when (val deparenthesized = KtPsiUtil.safeDeparenthesize(expression)) {
            is KtSimpleNameExpression -> addCamelNames(deparenthesized.getReferencedName(), validator)
            is KtQualifiedExpression -> addNamesByExpressionPSI(deparenthesized.selectorExpression, validator)
            is KtCallExpression -> addNamesByExpressionPSI(deparenthesized.calleeExpression, validator)
            is KtPostfixExpression -> addNamesByExpressionPSI(deparenthesized.baseExpression, validator)
        }
    }

    protected fun MutableCollection<String>.addName(name: String?, validator: (String) -> Boolean) {
        if (name == null) return
        val correctedName = when {
            name.isIdentifier() -> name
            name == "class" -> "clazz"
            else -> return
        }
        add(suggestNameByName(correctedName, validator))
    }


    private fun String.withPrefix(prefix: String): String {
        if (isEmpty()) return prefix
        val c = this[0]
        return (if (c in 'a'..'z') prefix.decapitalizeAsciiOnly()
        else prefix.capitalizeAsciiOnly()) + capitalizeAsciiOnly()
    }

    companion object {
        private const val MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS = 1000
        private val COMMON_TYPE_PARAMETER_NAMES = listOf("T", "U", "V", "W", "X", "Y", "Z")
        private val ACCESSOR_PREFIXES = arrayOf("get", "is", "set")
    }
}