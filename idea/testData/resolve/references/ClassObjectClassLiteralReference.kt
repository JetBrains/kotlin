package t

class A {
    companion object Named {
    }
}

fun f() {
    <caret>A.Named
}

// REF: (t).A