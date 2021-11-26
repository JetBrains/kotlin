// FIR_IDENTICAL
// WITH_EXTENDED_CHECKERS
fun <NN: Any, NNN: NN> nonMisleadingNullable(
        nn: NN?,
        nnn: NNN?
) {}

fun <T, N: T, INDIRECT: N> misleadingNullableSimple(
        t: T?,
        t2: T?,
        n: N?,
        ind: INDIRECT?
) {}

fun <T> interactionWithRedundant(t: T?<!REDUNDANT_NULLABLE!>?<!>) {}
