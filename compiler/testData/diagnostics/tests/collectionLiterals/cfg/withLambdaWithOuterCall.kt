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

fun take(w: WithInlineOf) = Unit
fun takeL(w: List<*>) = Unit

// graphs for `testWith` and `refWith` should be identical

fun testWith() {
    take([{ }])
}

fun refWith() {
    take(of({ }))
}

// graphs for `testWithout` and `refWithout` should be identical

fun testWithout() {
    takeL([{ }])
}

fun refWithout() {
    takeL(listOf({ }))
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionalType, inline, lambdaLiteral,
objectDeclaration, operator, starProjection, vararg */
