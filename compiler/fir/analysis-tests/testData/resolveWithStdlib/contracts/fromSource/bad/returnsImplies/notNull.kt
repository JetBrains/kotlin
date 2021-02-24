import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo(x: String?): Any? {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns(null) implies (x != null)
    }<!>

    if (true) {
        throw java.lang.IllegalArgumentException()
    }

    return x
}

@OptIn(ExperimentalContracts::class)
fun bar(x: String?): Any? {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns() implies (x != null)
    }<!>

    if (x == null) {
        return x
    }
    return x
}

@OptIn(ExperimentalContracts::class)
fun baz(x: String?): Any? {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns(null) implies (x != null)
    }<!>

    return x
}