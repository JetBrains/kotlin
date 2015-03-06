package t

class A {
    class object B {

    }
}

val A.B.bar : Int get() = 1

fun test() {
    <caret>A.bar
}


// REF: class object of (t).A

