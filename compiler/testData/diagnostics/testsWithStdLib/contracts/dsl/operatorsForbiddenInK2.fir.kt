// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +AllowContractsOnSomeOperators

import kotlin.contracts.*

// equals / compareTo

class A(var v: Int = 0) {
    override fun equals(other: Any?): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(true) implies (other is A) }
        return this.v == (other as? A)?.v
    }
}

operator fun A.compareTo(a: A?): Int {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (a != null) }
    return this.v.compareTo(a!!.v)
}

fun test_equals_and_compare(a1: A?, a2: A?) {
    val a = A()
    if (a.equals(a1)) {
        a1.v
    }
    if (a < a2) {
        a2.v
    }
}

// delegate operators

class Delegate(var value: String) {
    operator fun getValue(thisRef: Any?, property: Any?): String {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (property != null) }
        return value
    }

    operator fun setValue(thisRef: Any?, property: Any?, newValue: String) {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (property != null) }
        value = newValue
    }
}

class DelegateProvider(val value: String) {
    operator fun provideDelegate(thisRef: Any?, property: Any?): Delegate {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (property != null) }
        return Delegate(value)
    }
}

fun test_delegates() {
    var testMember by DelegateProvider("OK")
    testMember
    testMember = ""
}
