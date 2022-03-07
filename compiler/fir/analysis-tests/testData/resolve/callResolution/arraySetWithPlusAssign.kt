// ISSUE: KT-50861

/*
 * a[b] += c desugars to:
 *
 * 1. a.get(b).plusAssign(c)
 * 2. a.set(b, a.get(b).plus(c))
 */

// only plusAssign, no set
class A {
    operator fun get(i: Int): A = this
    operator fun plusAssign(v: () -> Unit) {}
}

fun test_1(x: A) {
    x[1] += {
        someCallInsideLambda()
        x[1] += {
            someCallInsideLambda()
            Unit
        }
    }
}

// only plusAssign, plus doesn't fit
class B {
    operator fun get(i: Int): B = this
    operator fun plusAssign(a: () -> Unit) {}
    operator fun plus(x: String) {}
}

fun test_2(x: B) {
    x[1] += {
        someCallInsideLambda()
        x[1] += {
            someCallInsideLambda()
            Unit
        }
    }
}

// only plusAssign, set doesn't fit
class C {
    operator fun get(i: Int): C = this
    operator fun set(i: Int, v: String) {}
    operator fun plusAssign(a: () -> Unit) {}
    operator fun plus(v: () -> Unit) {}
}

fun test_3(x: C) {
    x[1] += {
        someCallInsideLambda()
        x[1] += {
            someCallInsideLambda()
            Unit
        }
    }
}

// both fit
class D {
    operator fun set(i: Int, x: D) {}
    operator fun get(i: Int): D = this
    operator fun plusAssign(x: () -> Unit) {}
    operator fun plus(v: () -> Unit): D = this
}

fun test_4(x: D) {
    x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
        someCallInsideLambda()
        x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
            someCallInsideLambda()
            Unit
        }
    }
}

// nothing fit
class E

fun test_5(x: E) {
    x<!NO_GET_METHOD!>[1]<!> += {
        someCallInsideLambda()
        x<!NO_GET_METHOD!>[1]<!> += {
            someCallInsideLambda()
            Unit
        }
    }
}


// only plus, plusAssign doesn't fir because of lambda
class F {
    operator fun set(i: Int, x: F) {}
    operator fun get(i: Int): F = this
    operator fun plusAssign(x: () -> String) {}
    operator fun plus(v: () -> Int): F = this
}

fun test_6(x: F) {
    x[1] <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> {
        someCallInsideLambda()
        "please choose String"
    }
}

// only plus, no set. 3 indices in get/set
class G {
    operator fun get(i: Int, j: Int, k: Int): G = this
    operator fun set(i: Int, j: Int, k: Int, x: G) {}
    operator fun plus(v: () -> Unit): G = this
}

fun test_7(x: G) {
    x[1, 2, 3] += {
        someCallInsideLambda()
        x[1, 2, 3] += {
            someCallInsideLambda()
            Unit
        }
    }
}

fun someCallInsideLambda() {}
