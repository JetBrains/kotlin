package t

class A {
    default object B {

    }
}

val A.B.bar : Int get() = 1

fun test() {
    <caret>A.bar
}


// REF: default object of (t).A

