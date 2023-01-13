// !OPT_IN: kotlin.RequiresOptIn
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun testAlwaysNotNull(x: String?): Any? {
    contract {
        returnsNotNull() implies (x is String && <!SENSELESS_COMPARISON!>x != null<!>)
    }

    return x
}

@OptIn(ExperimentalContracts::class)
fun testAlwaysAny(x: String?): Any? {
    contract {
        returnsNotNull() implies (<!USELESS_IS_CHECK!>x is String?<!> || <!USELESS_IS_CHECK!>x is Any?<!>)
    }

    return x
}