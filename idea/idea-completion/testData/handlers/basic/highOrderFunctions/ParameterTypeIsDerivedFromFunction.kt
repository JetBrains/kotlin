interface I : () -> Unit

fun foo(i: I){}

fun bar() {
    <caret>
}

// ELEMENT: foo
