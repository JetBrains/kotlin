/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.junit.Assert
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

fun verifyDiagnostics(vararg diagnosticContainers: KtDiagnosticsContainer) {
    val errors = mutableListOf<String>()
    val existingDiagnosticFactories = mutableMapOf<String, AbstractKtDiagnosticFactory>()
    for (container in diagnosticContainers) {
        container.getRendererFactory().MAP.verifyMessages(container, errors, existingDiagnosticFactories)
    }
    if (errors.isNotEmpty()) {
        Assert.fail(
            errors.joinToString(
                "\n\n",
                postfix = "\n\nSee https://youtrack.jetbrains.com/articles/KT-A-610 for the style guide.\n\n"
            )
        )
    }
}

private fun KtDiagnosticFactoryToRendererMap.verifyMessages(
    container: KtDiagnosticsContainer,
    errors: MutableList<String>,
    existingDiagnosticFactories: MutableMap<String, AbstractKtDiagnosticFactory>
) {
    for (property in container::class.memberProperties) {
        when (val factory = property.getter.call(container)) {
            is AbstractKtDiagnosticFactory -> {
                errors += verifyMessageForFactory(factory, property, existingDiagnosticFactories)
            }
            is KtDiagnosticFactoryForDeprecation<*> -> {
                errors += verifyMessageForFactory(factory.warningFactory, property, existingDiagnosticFactories)
                errors += verifyMessageForFactory(factory.errorFactory, property, existingDiagnosticFactories)
            }
            else -> {}
        }
    }
}

private val messageParameterRegex = """\{\d.*?}""".toRegex()

private val lastCharRegex = """[.}\d]""".toRegex()

private val lastCharExclusions = listOf(
    FirErrors.DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED.name,
    FirErrors.ERROR_SUPPRESSION.name,
    FirErrors.NOT_A_MULTIPLATFORM_COMPILATION.name,
    FirErrors.CONTEXT_CLASS_OR_CONSTRUCTOR.name,
)

private val uselessInIdExclusions = listOf(
    FirErrors.USELESS_CAST.name,
    FirErrors.USELESS_ELVIS.name,
    FirErrors.USELESS_ELVIS_LEFT_IS_NULL.name,
    FirErrors.USELESS_ELVIS_RIGHT_IS_NULL.name,
    FirErrors.USELESS_IS_CHECK.name,
    FirErrors.USELESS_VARARG_ON_PARAMETER.name,
    FirErrors.USELESS_CALL_ON_NOT_NULL.name,
    FirJvmErrors.USELESS_JVM_EXPOSE_BOXED.name,
    FirJsErrors.JS_NO_RUNTIME_USELESS_ON_EXTERNAL_INTERFACE.name,
)

fun KtDiagnosticFactoryToRendererMap.verifyMessageForFactory(
    factory: AbstractKtDiagnosticFactory,
    property: KProperty<*>,
    existingDiagnosticFactories: MutableMap<String, AbstractKtDiagnosticFactory>,
): List<String> {
    return buildList {
        val renderer = get(factory) ?: run {
            add("No default diagnostic renderer is provided for ${property.name}")
            return@buildList
        }

        val existingDiagnosticFactory = existingDiagnosticFactories[factory.name]
        if (existingDiagnosticFactory != null) {
            add("The diagnostic ${factory.name} is declared both in ${existingDiagnosticFactory.rendererFactory::class.simpleName} and ${factory.rendererFactory::class.simpleName}")
        } else {
            existingDiagnosticFactories[factory.name] = factory
        }

        val message = renderer.message

        if (factory is KtSourcelessDiagnosticFactory) {
            if (message != BaseSourcelessDiagnosticRendererFactory.MESSAGE_PLACEHOLDER) {
                add(
                    """
                    ${KtSourcelessDiagnosticFactory::class.simpleName} currently supports only `${BaseSourcelessDiagnosticRendererFactory.MESSAGE_PLACEHOLDER}` placeholder which implies passing particular messages directly to a reporter.
                    The current value of ${property.name} is `${message}`.
                    """.trimIndent()
                )
            }
            return@buildList
        }

        val parameterCount = when (renderer) {
            is KtDiagnosticWithParameters4Renderer<*, *, *, *> -> 4
            is KtDiagnosticWithParameters3Renderer<*, *, *> -> 3
            is KtDiagnosticWithParameters2Renderer<*, *> -> 2
            is KtDiagnosticWithParameters1Renderer<*> -> 1
            is KtSourcelessDiagnosticRenderer -> 1
            is SimpleKtDiagnosticRenderer -> 0
        }

        checkRules(property.name, message, parameterCount)
    }
}

fun MutableList<String>.checkRules(name: String, message: String, parameterCount: Int) {
    for (parameter in messageParameterRegex.findAll(message)) {
        val index = parameter.value.substring(1, 2).toInt()
        if (index >= parameterCount) {
            add("Message for $name references wrong parameter {$index}")
        }
    }

    if (parameterCount > 0 && message.contains("(?<!')'(?!')".toRegex())) {
        add("Renderer for $name has parameters and contains a single quote. Text inside single quotes is not formatted in MessageFormat. Use double quotes instead.")
    }

    if (parameterCount == 0 && message.contains("(?<!')''(?!')".toRegex())) {
        add("Renderer for $name has no parameters and contains double quote. Single quotes should be used.")
    }

    if (name !in lastCharExclusions && !message.last().toString().matches(lastCharRegex)) {
        add("Renderer for $name should end with a full stop. If this error is a false positive, add the name of the diagnostic to the list of exclusions.")
    }

    checkRule(
        name,
        message,
        """\b(colour|favour|realise|analyse|centre|defence|offence|licence|cancelled|metre|tonne|cheque|catalogue|neighbour|grey|programme)\b""".toRegex(
            RegexOption.IGNORE_CASE),
        "uses British spelling. Use American spelling instead"
    )

    checkRule(
        name,
        message,
        """\b(?:we|us|you(?!\s+have))\b""".toRegex(RegexOption.IGNORE_CASE),
        "uses 'we', 'us' or 'you'.",
        setOf(
            FirErrors.CONTEXT_RECEIVERS_DEPRECATED.name,
            FirErrors.NO_TYPE_ARGUMENTS_ON_RHS.name,
            "PARCELABLE_TYPE_NOT_SUPPORTED",
            FirErrors.ROOT_IDE_PACKAGE_DEPRECATED.name,
        )
    )
    checkRule(
        name,
        message,
        """\bplease\b""".toRegex(RegexOption.IGNORE_CASE),
        "uses overly polite tone",
        setOf(
            FirErrors.CONTEXT_RECEIVERS_DEPRECATED.name,
            FirErrors.ERROR_SUPPRESSION.name,
            FirErrors.ROOT_IDE_PACKAGE_DEPRECATED.name,
        )
    )

    checkRule(
        name,
        message,
        """\b(?:probably|likely|maybe|certainly|possibly|undoubtedly|presumably|apparently|hopefully)\b""".toRegex(RegexOption.IGNORE_CASE),
        "uses adverb of probability (likely, maybe, ...)",
    )

    checkRule(
        name,
        message,
        """\b(?:could|should|would|shall)\b""".toRegex(RegexOption.IGNORE_CASE),
        "uses modal verb (could, should, ...) with uncertainty",
        setOf(
            FirErrors.VERSION_REQUIREMENT_DEPRECATION.name,
            FirErrors.NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE.name
        )
    )

    checkRule(
        name,
        message,
        """\b(?:must|ca|is|wo|do)n''?t\b""".toRegex(RegexOption.IGNORE_CASE),
        "uses contraction",
    )

    checkRule(
        name,
        message,
        """\bmust not\b""".toRegex(RegexOption.IGNORE_CASE),
        "uses 'must not'. Replace with 'cannot'",
    )

    checkRule(
        name,
        message,
        """\buseless\b""".toRegex(RegexOption.IGNORE_CASE),
        "uses 'useless'. Use 'redundant' if the problem doesn't affect the compilation result " +
                "or describe the problem more precisely (e.g. 'unreachable else' instead of 'useless else'",
    )

    if (name.contains("useless", ignoreCase = true) && name !in uselessInIdExclusions) {
        add("Diagnostic ID '$name' contains the term 'useless'.")
    }
}


private fun MutableList<String>.checkRule(name: String, message: String, regex: Regex, hasProblem: String, exclusions: Set<String> = emptySet()) {
    if (name !in exclusions && message.contains(regex)) {
        val updatedMessage = message.replace(regex) { matchResult -> "[[${matchResult.value}]]" }
        add(
            "Message of $name $hasProblem:\n$updatedMessage\nIf this error is a false positive, add the name of the diagnostic to the list of exclusions."
        )
    }
}
