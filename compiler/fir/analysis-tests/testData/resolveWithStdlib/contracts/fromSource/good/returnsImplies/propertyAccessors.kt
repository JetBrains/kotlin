import kotlin.contracts.*

interface A {
    fun foo()
}

var Any?.isNotNull: Boolean
    get() {
        contract {
            returns(true) implies (this@isNotNull != null)
        }
        return this != null
    }
    set(value) {
        contract {
            returns() implies (this@isNotNull != null)
            <!ERROR_IN_CONTRACT_DESCRIPTION!>require(<!SENSELESS_COMPARISON!>this != null<!>)<!>
        }
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
