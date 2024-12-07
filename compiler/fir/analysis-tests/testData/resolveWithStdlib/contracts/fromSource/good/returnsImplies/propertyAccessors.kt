// RUN_PIPELINE_TILL: FRONTEND
import kotlin.contracts.*

interface A {
    fun foo()
}

@OptIn(ExperimentalContracts::class)
var Any?.isNotNull: Boolean
    get() {
        <!CONTRACT_NOT_ALLOWED!>contract<!> {
            returns(true) implies (this@isNotNull != null)
        }
        return this != null
    }
    set(value) {
        <!CONTRACT_NOT_ALLOWED!>contract<!> {
            <!WRONG_IMPLIES_CONDITION!>returns() implies (this@isNotNull != null)<!>
            require(<!SENSELESS_COMPARISON!>this != null<!>)
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
