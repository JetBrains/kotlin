package t

class A {
    default object Named {
        val i: Int
    }
}

fun f() {
    <caret>A.Named.i
}

// REF: (t).A