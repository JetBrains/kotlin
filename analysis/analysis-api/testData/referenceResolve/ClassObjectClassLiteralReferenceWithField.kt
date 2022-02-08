package t

class A {
    companion object Named {
        val i: Int = 10
    }
}

fun f() {
    <caret>A.Named.i
}