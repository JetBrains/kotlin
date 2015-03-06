package t

class A {
    class object Named {
    }
}

fun f() {
    <caret>A.Named
}

// REF: (t).A