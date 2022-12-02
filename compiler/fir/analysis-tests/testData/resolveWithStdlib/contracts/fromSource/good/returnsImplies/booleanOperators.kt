import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun myRequire(b: Boolean) {
    contract {
        returns() implies (b)
    }
    if (!b) throw IllegalStateException()
}

@OptIn(ExperimentalContracts::class)
fun myRequireAnd(b1: Boolean, b2: Boolean) {
    contract {
        returns() implies (b1 && b2)
    }
    if (!(b1 && b2)) throw IllegalStateException()
}

@OptIn(ExperimentalContracts::class)
fun myRequireOr(b1: Boolean, b2: Boolean) {
    contract {
        returns() implies (b1 || b2)
    }
    if (!(b1 || b2)) throw IllegalStateException()
}

@OptIn(ExperimentalContracts::class)
fun myRequireNot(b: Boolean) {
    contract {
        returns() implies (!b)
    }
    if (b) throw IllegalStateException()
}

// ----------------------------------------------------

interface A {
    fun foo()
}

interface B : A {
    fun bar()
}

interface C : A {
    fun baz()
}

// ----------------------------------------------------

fun test_1(x: Any) {
    myRequire(x is A)
    x.foo()
}

fun test_2(x: Any) {
    myRequireAnd(x is B, x is C)
    x.foo()
    x.bar()
    x.baz()
}

fun test_3(x: Any) {
    myRequireOr(x is B, x is C)
    x.foo() // OK
    x.<!UNRESOLVED_REFERENCE!>bar<!>() // Error
    x.<!UNRESOLVED_REFERENCE!>baz<!>() // Error
}

fun test_4(x: Any) {
    myRequireNot(x !is A)
    x.foo()
}