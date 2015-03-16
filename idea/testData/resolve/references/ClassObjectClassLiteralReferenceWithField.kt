package t

class A {
    companion object Named {
        val i: Int
    }
}

fun f() {
    <caret>A.Named.i
}

// REF: (t).A