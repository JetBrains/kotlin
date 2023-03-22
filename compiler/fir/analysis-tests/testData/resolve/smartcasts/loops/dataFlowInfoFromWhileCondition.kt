// IGNORE_REVERSED_RESOLVE
// !DUMP_CFG
interface A {
    fun foo(): Boolean
}
interface B : A
interface C : A

fun test() {
    var a: A? = null
    while (a is B || a is C) {
        a.foo()
    }
}
