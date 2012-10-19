fun nonMisleadingNullable<NN: Any, NNN: NN>(
        <!UNUSED_PARAMETER!>nn<!>: NN?,
        <!UNUSED_PARAMETER!>nnn<!>: NNN?
) {}

fun twoBounds<NN: Any, TWO_BOUNDS: Any>(
        <!UNUSED_PARAMETER!>tb<!>: TWO_BOUNDS?

) where TWO_BOUNDS : NN {}

fun misleadingNullableSimple<T, N: T, INDIRECT: N>(
        <!UNUSED_PARAMETER!>t<!>: T<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>,
        <!UNUSED_PARAMETER!>t2<!>: T<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>,
        <!UNUSED_PARAMETER!>n<!>: N<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>,
        <!UNUSED_PARAMETER!>ind<!>: INDIRECT<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>
) {}

fun misleadingNullableMultiBound<FIRST_BOUND: Any?, SECOND_BOUND: Any>(
        <!UNUSED_PARAMETER!>fb<!>: FIRST_BOUND<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>,
        <!UNUSED_PARAMETER!>sb<!>: SECOND_BOUND<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!>
) where FIRST_BOUND: Any, SECOND_BOUND: Any? {
}

fun interactionWithRedundant<T>(<!UNUSED_PARAMETER!>t<!>: T<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!><!REDUNDANT_NULLABLE!>?<!>) {}