package t

class A {
    default object Default {

    }
}

fun A.Default.foo() {}

fun test() {
    <caret>A.foo()
}


// REF: default object of (t).A

