// !OPT_IN: kotlin.RequiresOptIn
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun test1(x: String?): Int? {
    contract {
        returnsNotNull() implies (x != null)
    }

    return x?.length
}

@OptIn(ExperimentalContracts::class)
fun test2(x: String?): Int? {
    contract {
        <!WRONG_IMPLIES_CONDITION!>returnsNotNull() implies (<!USELESS_IS_CHECK!>x is Boolean<!>)<!>
    }

    return x?.length
}