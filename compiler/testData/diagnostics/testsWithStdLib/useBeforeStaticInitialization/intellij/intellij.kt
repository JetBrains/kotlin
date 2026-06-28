// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB

// FILE: HighlightDisplayLevel.kt
package com.jetbrains.cyclicexamples.komi

import kotlin.jvm.JvmField

open class HighlightDisplayLevel(severity: Int) {

    var severity: Int = severity
        private set

    var icon: Any = "test"
        private set
    var outlineIcon: Any = "test"
        private set

    constructor(severity: Int, icon: Any) : this(severity = severity, icon = icon, outlineIcon = icon)

    private constructor(severity: Int, icon: Any, outlineIcon: Any) : this(severity) {
        this.icon = icon
        this.outlineIcon = outlineIcon
    }

    companion object {

        private val DO_NOT_SHOW_KEY = "DO_NOT_SHOW"

        private fun createHighlightDisplayLevel(
            severity: Int,
            key: String,
            icon: Any,
            outlineIcon: Any,
        ): HighlightDisplayLevel {
            return HighlightDisplayLevel(
                severity = severity,
                icon = icon,
                outlineIcon = outlineIcon,
            )
        }

        @JvmField
        val DO_NOT_SHOW: HighlightDisplayLevel = HighlightDisplayLevel(severity = 0, icon = "empty")

        @JvmField
        val CONSIDERATION_ATTRIBUTES: HighlightDisplayLevel = HighlightDisplayLevel(severity = 4, icon = "empty")

        @Suppress("DEPRECATION")
        @JvmField
        @Deprecated("use {@link #WEAK_WARNING} instead")
        val INFO: HighlightDisplayLevel = createHighlightDisplayLevel(severity = 0,
            key = DO_NOT_SHOW_KEY, icon = "inspections",
            outlineIcon = "inspections")

        @JvmField
        val WEAK_WARNING: HighlightDisplayLevel =
            createHighlightDisplayLevel(
                severity = 0,
                key = "weakWarning",
                icon = "inspections",
                outlineIcon = "inspections",
            )

        @JvmField
        val NON_SWITCHABLE_ERROR: HighlightDisplayLevel = object : HighlightDisplayLevel(2) {
            override val isNonSwitchable: Boolean
                get() = true
        }

        @JvmField
        val NON_SWITCHABLE_WARNING: HighlightDisplayLevel = object : HighlightDisplayLevel(0) {
            override val isNonSwitchable: Boolean
                get() = true
        }

    }

    open val isNonSwitchable: Boolean
        get() = false
}

// FILE: DslStyleUtils.kt
object DslStyleUtils {
    private const val STYLE_COUNT = 4

    private val STYLE_KEYS: List<String> = listOf(
        "CUSTOM_KEYWORD1_ATTRIBUTES",
        "CUSTOM_KEYWORD2_ATTRIBUTES",
        "CUSTOM_KEYWORD3_ATTRIBUTES",
        "CUSTOM_KEYWORD4_ATTRIBUTES"
    )

    private val styles: List<String> = (1..STYLE_COUNT).map { index ->
        externalKeyName(index) + STYLE_KEYS[index - 1]
    }

    /**
     * highlight DSL errors as slightly more severe than HighlightInfoType.SYMBOL_TYPE_SEVERITY,
     * to avoid conflicts with [org.jetbrains.kotlin.idea.highlighting.visitor.KotlinFunctionCallSemanticHighlightingVisitor],
     * which highlights exactly the same function calls with HighlightInfoType.SYMBOL_TYPE_SEVERITY
     */
    private val DSL_TYPE_SEVERITY = "DSL_TYPE_SEVERITY"

    internal val types: List<String> = styles.map { attributeKey ->
        DSL_TYPE_SEVERITY + attributeKey
    }

    val descriptionsToStyles: Map<String, String> = (1..STYLE_COUNT).associate { index ->
        "highlighter.name.dsl" + styleOptionDisplayName(index) to styleById(index)
    }

    private fun externalKeyName(index: Int) = "KOTLIN_DSL_STYLE$index"

    fun styleOptionDisplayName(index: Int): String = "highlighter.name.style$index"

    fun styleById(styleId: Int): String = styles[styleId - 1]
}

/* GENERATED_FIR_TAGS: additiveExpression, anonymousObjectExpression, assignment, classDeclaration, companionObject,
const, functionDeclaration, getter, integerLiteral, lambdaLiteral, objectDeclaration, override, primaryConstructor,
propertyDeclaration, rangeExpression, secondaryConstructor, stringLiteral, thisExpression */
