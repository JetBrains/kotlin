// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_MESSAGES

open class PhantomEquivalence {
    override fun equals(other: Any?) = other is PhantomEquivalence
}

sealed interface Variants {
    object A : Variants
    object B : PhantomEquivalence(), Variants
    data object C : Variants
    data object D : PhantomEquivalence(), Variants
}

fun foo(v: Variants): String {
    if (v == Variants.A) {
        return "A"
    }

    return when (v) {
        <!UNSAFE_EXHAUSTIVENESS("Variants.B")!>Variants.B<!> -> "B"
        Variants.C -> "C"
        <!UNSAFE_EXHAUSTIVENESS("Variants.D")!>Variants.D<!> -> "D"
    }
}

fun bar(v: Variants): String {
    if (v == Variants.A) {
        return "A"
    }

    return when (v) {
        Variants.B -> "B"
        Variants.C -> "C"
        Variants.D -> "D"
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> "A?"
    }
}

sealed class Options {
    data object A : Options()
    data class B(val it: Int) : Options() {
        override fun equals(other: Any?) = other is B
    }
}

fun baz(v: Options): String {
    if (v == Options.A) {
        return "A"
    }

    return when (v) {
        is Options.B -> "B"
    }
}
