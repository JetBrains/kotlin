// LL_FIR_DIVERGENCE
// LL FIR does not produce VIRTUAL_MEMBER_HIDDEN for this expect/actual scenario
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-32840

// KT-32840: Declarations clash in hierarchical multiplatform projects on override

// MODULE: common
package sample

expect interface Input

abstract class AbstractInput : Input {
    val head: Int = 0

    fun foo(): Int = 1
}

// MODULE: platform()()(common)
package sample

actual interface Input {
    fun foo(): Int = 2
}

class JSInput : AbstractInput() {
    // what `foo` is allowed here?
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, integerLiteral, interfaceDeclaration,
propertyDeclaration */
