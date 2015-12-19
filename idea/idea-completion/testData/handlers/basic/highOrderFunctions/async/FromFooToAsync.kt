fun asyFoo(a: () -> Unit) {}

fun async(a: () -> Unit) {}

fun test() {
    asy<caret>Foo {  }
}

// ELEMENT: async
// CHAR: '\t'