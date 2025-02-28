fun foo() {
    bar() // unresolved

    return object : Foo
}

fun bar() {}


// COMPILATION_ERRORS
