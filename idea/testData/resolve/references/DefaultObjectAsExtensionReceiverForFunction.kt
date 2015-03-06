package t

class A {
    class object Default {

    }
}

fun A.Default.foo() {}

fun test() {
    <caret>A.foo()
}


// REF: class object of (t).A

