fun <NN: Any, NNN: NN> nonMisleadingNullable(
        <!UNUSED_PARAMETER!>nn<!>: NN?,
        <!UNUSED_PARAMETER!>nnn<!>: NNN?
) {}

fun <NN: Any, TWO_BOUNDS> twoBounds(
        <!UNUSED_PARAMETER!>tb<!>: TWO_BOUNDS?

) where TWO_BOUNDS: Any, TWO_BOUNDS : NN {}

fun <T, N: T, INDIRECT: N> misleadingNullableSimple(
        <!UNUSED_PARAMETER!>t<!>: T<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>,
        <!UNUSED_PARAMETER!>t2<!>: T<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>,
        <!UNUSED_PARAMETER!>n<!>: N<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>,
        <!UNUSED_PARAMETER!>ind<!>: INDIRECT<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>
) {}

fun <FIRST_BOUND, SECOND_BOUND> misleadingNullableMultiBound(
        <!UNUSED_PARAMETER!>fb<!>: FIRST_BOUND<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>,
        <!UNUSED_PARAMETER!>sb<!>: SECOND_BOUND<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>
) where FIRST_BOUND: Any?, FIRST_BOUND: Any, SECOND_BOUND: Any, SECOND_BOUND: Any? {
}

fun <T> interactionWithRedundant(<!UNUSED_PARAMETER!>t<!>: T<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!><!REDUNDANT_NULLABLE!>?<!>) {}