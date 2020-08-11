import kotlin.contracts.*

interface A {
    fun foo()
}

var Any?.isNotNull: Boolean
    get() {
        <!WRONG_IMPLIES_CONDITION!>contract {
            returns(true) implies (this@isNotNull != null)
        }<!>
        return this != null
    }
    set(value) {
        <!WRONG_IMPLIES_CONDITION!>contract {
            returns() implies (this@isNotNull != null)
            require(this != null)
        }<!>
    }

fun test_1(a: A?) {
    if (a.isNotNull) {
        a.foo()
    }
}

fun test_2(a: A?) {
    a.isNotNull = true
    a.foo()
}