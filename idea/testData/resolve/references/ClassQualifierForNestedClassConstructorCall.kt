package t

class A {
    class object Default {

    }

    class B
}

fun test() {
    <caret>A.B()
}


// REF: (t).A

