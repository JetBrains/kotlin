// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.RequiresOptIn
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo1(x: Any): Boolean {
    contract {
        returns(true) implies (x !is Int)
    }
    return (x is String)
}

@OptIn(ExperimentalContracts::class)
fun foo2(x: Any): Boolean {
    contract {
        <!WRONG_IMPLIES_CONDITION!>returns(true) implies (x !is String)<!>
    }
    return (x is String)
}

@OptIn(ExperimentalContracts::class)
fun foo3(x: Any?): Boolean {
    contract {
        <!WRONG_IMPLIES_CONDITION!>returns(true) implies (x !is Any)<!>
    }
    return (x is String)
}

sealed class Sealed {
    data class A(val a: Int): Sealed()
    data class B(val b: Int): Sealed()
    data class C(val c: Int): Sealed()
}

@OptIn(ExperimentalContracts::class)
fun bar1(x: Sealed): Boolean {
    contract {
        returns(true) implies (x !is Sealed.B)
    }
    return (x !is Sealed.B)
}

@OptIn(ExperimentalContracts::class)
fun bar2(x: Sealed): Boolean {
    contract {
        returns(true) implies (x !is Sealed.B)
    }
    return (x is Sealed.A)
}

@OptIn(ExperimentalContracts::class)
fun bar3(x: Sealed): Boolean {
    contract {
        // currently does not work
        <!WRONG_IMPLIES_CONDITION!>returns(true) implies (x !is Sealed.B)<!>
    }
    return (x is Sealed.A || x is Sealed.C)
}

@OptIn(ExperimentalContracts::class)
fun bar4(x: Sealed): Boolean {
    contract {
        <!WRONG_IMPLIES_CONDITION!>returns(true) implies (x !is Sealed.B)<!>
    }
    return (x !is Sealed.A)
}