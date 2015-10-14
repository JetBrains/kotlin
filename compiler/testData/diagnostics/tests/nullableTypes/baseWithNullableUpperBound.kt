fun <NN: Any, NNN: NN> nonMisleadingNullable(
        <!UNUSED_PARAMETER!>nn<!>: NN?,
        <!UNUSED_PARAMETER!>nnn<!>: NNN?
) {}

fun <NN: Any, TWO_BOUNDS> twoBounds(
        <!UNUSED_PARAMETER!>tb<!>: TWO_BOUNDS?

) where TWO_BOUNDS: Any, TWO_BOUNDS : NN {}

fun <T, N: T, INDIRECT: N> misleadingNullableSimple(
        <!UNUSED_PARAMETER!>t<!>: T?,
        <!UNUSED_PARAMETER!>t2<!>: T?,
        <!UNUSED_PARAMETER!>n<!>: N?,
        <!UNUSED_PARAMETER!>ind<!>: INDIRECT?
) {}

fun <FIRST_BOUND, SECOND_BOUND> misleadingNullableMultiBound(
        <!UNUSED_PARAMETER!>fb<!>: FIRST_BOUND?,
        <!UNUSED_PARAMETER!>sb<!>: SECOND_BOUND?
) where FIRST_BOUND: Any?, FIRST_BOUND: Any, SECOND_BOUND: Any, SECOND_BOUND: Any? {
}

fun <T> interactionWithRedundant(<!UNUSED_PARAMETER!>t<!>: T?<!REDUNDANT_NULLABLE!>?<!>) {}