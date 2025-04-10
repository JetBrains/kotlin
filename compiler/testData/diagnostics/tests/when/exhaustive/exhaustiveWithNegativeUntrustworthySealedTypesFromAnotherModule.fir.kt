// RUN_PIPELINE_TILL: BACKEND
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

    return when (v) {
        <!UNSAFE_EXHAUSTIVENESS("Variants.B")!>Variants.B<!> -> "B"
        Variants.C -> "C"
        <!UNSAFE_EXHAUSTIVENESS("Variants.D")!>Variants.D<!> -> "D"
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
