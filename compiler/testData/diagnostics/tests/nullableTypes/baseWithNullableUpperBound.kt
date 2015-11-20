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

fun <T> interactionWithRedundant(<!UNUSED_PARAMETER!>t<!>: T?<!REDUNDANT_NULLABLE!>?<!>) {}