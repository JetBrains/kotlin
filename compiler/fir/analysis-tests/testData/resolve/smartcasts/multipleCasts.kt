// IGNORE_REVERSED_RESOLVE
// !DUMP_CFG
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
