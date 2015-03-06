package t

class A {
    default object Default {

    }

    class B
}

fun test() {
    <caret>A.B()
}


// REF: (t).A

