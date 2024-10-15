// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A {
    var b = 1
}

fun Any.test() {
    (this as A).b += 1 <!USELESS_CAST!>as Int<!>
}