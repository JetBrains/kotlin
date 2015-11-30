fun <NN: Any, NNN: NN> nonMisleadingNullable(
        <!UNUSED_PARAMETER!>nn<!>: NN?,
        <!UNUSED_PARAMETER!>nnn<!>: NNN?
) {}

fun <T, N: T, INDIRECT: N> misleadingNullableSimple(
        <!UNUSED_PARAMETER!>t<!>: T?,
        <!UNUSED_PARAMETER!>t2<!>: T?,
        <!UNUSED_PARAMETER!>n<!>: N?,
        <!UNUSED_PARAMETER!>ind<!>: INDIRECT?
) {}

fun <T> interactionWithRedundant(<!UNUSED_PARAMETER!>t<!>: T?<!REDUNDANT_NULLABLE!>?<!>) {}