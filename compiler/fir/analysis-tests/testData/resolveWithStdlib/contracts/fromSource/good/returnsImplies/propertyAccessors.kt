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
            require(this != null)
        }
    }

fun test_1(a: A?) {
    if (a.isNotNull) {
        a.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    }
}

fun test_2(a: A?) {
    a.isNotNull = true
    a.<!INAPPLICABLE_CANDIDATE!>foo<!>()
}