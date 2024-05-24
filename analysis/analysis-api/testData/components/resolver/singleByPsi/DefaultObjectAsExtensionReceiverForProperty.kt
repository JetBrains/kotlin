package t

class A {
    companion object B {

    }
}

val A.B.bar : Int get() = 1

fun test() {
    <caret>A.bar
}



