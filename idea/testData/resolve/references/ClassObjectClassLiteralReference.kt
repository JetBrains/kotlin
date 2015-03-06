package t

class A {
    default object Named {
    }
}

fun f() {
    <caret>A.Named
}

// REF: (t).A