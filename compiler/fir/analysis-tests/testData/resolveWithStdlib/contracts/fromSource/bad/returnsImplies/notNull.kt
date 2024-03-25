// !OPT_IN: kotlin.RequiresOptIn
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo(x: String?): Any? {
    contract {
        <!WRONG_IMPLIES_CONDITION!>returns(null) implies (x != null)<!>
    }

    if (true) {
        throw java.lang.IllegalArgumentException()
    }

    return x
}

@OptIn(ExperimentalContracts::class)
fun bar(x: String?): Any? {
    contract {
        <!WRONG_IMPLIES_CONDITION!>returns() implies (x != null)<!>
    }

    if (x == null) {
        return x
    }
    return x
}

@OptIn(ExperimentalContracts::class)
fun baz(x: String?): Any? {
    contract {
        <!WRONG_IMPLIES_CONDITION!>returns(null) implies (x != null)<!>
    }

    return x
}