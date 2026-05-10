// LANGUAGE: +CollectionLiterals
// DUMP_CFG
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

import WithInlineOf.Companion.of

class WithInlineOf {
    companion object {
        inline operator fun of(first: () -> Unit, vararg other: () -> Unit): WithInlineOf = WithInlineOf()
    }
}

// graphs for `testWith` and `refWith` should be identical

fun testWith() {
    val with: WithInlineOf = [{ }]
}

fun refWith() {
    val with: WithInlineOf = of({ })
}

// graphs for `testWithout` and `refWithout` should be identical

fun testWithout() {
    val without = [{ }]
}

fun refWithout() {
    val without = listOf({ })
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionalType, inline, lambdaLiteral,
localProperty, objectDeclaration, operator, propertyDeclaration, vararg */
