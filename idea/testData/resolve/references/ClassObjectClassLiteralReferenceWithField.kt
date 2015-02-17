package t

class A {
    class object Named {
        val i: Int
    }
}

fun f() {
    <caret>A.Named.i
}

// REF: (t).A