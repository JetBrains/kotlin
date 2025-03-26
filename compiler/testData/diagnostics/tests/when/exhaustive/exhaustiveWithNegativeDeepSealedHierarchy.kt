// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND

sealed interface Variants {
    data object A : Variants

    sealed interface Subvariants : Variants {
        data object B : Subvariants
    }
}

fun foo(v: Variants): String {
    if (v is Variants.Subvariants) {
        return "B"
    }

    return <!NO_ELSE_IN_WHEN!>when<!> (v) {
        Variants.A -> "A"
    }
}
