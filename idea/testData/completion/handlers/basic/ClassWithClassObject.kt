class AClass {
    default object {}
}

fun foo() {
    bar(<caret>)
}

// ELEMENT: AClass
