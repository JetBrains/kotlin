// WITH_REFLECT
// ISSUE: KT-58136

class Box<out T>(val value: T)

fun test(param: Box<(Int) -> Unit>) {

}

fun foo() {
    val callback: (Int) -> Unit? = {}
    test(<!ARGUMENT_TYPE_MISMATCH!>Box(callback)<!>)
}
