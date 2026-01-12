// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +DataFlowBasedExhaustiveness
// MODULE: a

open class PhantomEquivalence {
    override fun equals(other: Any?) = other is PhantomEquivalence
}

sealed interface Variants {
    object A : Variants
    object B : PhantomEquivalence(), Variants
    data object C : Variants
    data object D : PhantomEquivalence(), Variants
}

sealed class Options {
    data object A : Options()
    data class B(val it: Int) : Options() {
        override fun equals(other: Any?) = other is B
    }
}

// MODULE: b(a)

fun foo(v: Variants): String {
    if (v == Variants.A) {
        return "A"
    }

    return <!WHEN_ON_SEALED!>when (v) {
        Variants.B -> "B"
        Variants.C -> "C"
        Variants.D -> "D"
    }<!>
}

fun baz(v: Options): String {
    if (v == Options.A) {
        return "A"
    }

    return <!WHEN_ON_SEALED!>when (v) {
        is Options.B -> "B"
    }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, data, equalityExpression, functionDeclaration, ifExpression,
interfaceDeclaration, isExpression, nestedClass, nullableType, objectDeclaration, operator, override, primaryConstructor,
propertyDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
