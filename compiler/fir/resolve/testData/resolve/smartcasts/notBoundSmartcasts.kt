// There would be ambiguities if some expression was smartcasted to (A & B) and foo() was called.
// There was a bug where 2 variables were "bound" together if they are assigned from the same function call or property.
interface A {
    fun foo(): Int
}
interface B {
    fun foo(): Int
}

fun getAny(): Any? = null

fun test_0() {
    val a = getAny()
    val b = getAny()
    a as A
    a.foo()
    b as B
    b.foo()
}

fun test_1() {
    val a = getAny()
    val b = getAny()
    if (a is A && b is B) {
        a.foo()
        b.foo()
    }
}

class D(val any: Any?)

fun Any.baz() {}

fun test_2(d: D) {
    // Elvis operator is converted into == function call
    val a = d.any ?: return
    a.baz()
    d.any.baz()
    a as A
    a.foo()
}

fun test_3(d1: D) {
    val a = d1.any
    a as A
    a.foo()
    d1.any.foo()
    d1.any.baz()
}

fun test_4(d1: D, d2: D) {
    val a = d1?.any
    val b = d2?.any
    a as A
    a.foo()
    b as B
    b.foo()  // should be OK
}
