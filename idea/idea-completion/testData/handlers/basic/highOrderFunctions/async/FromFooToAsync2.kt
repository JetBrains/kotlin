fun asyFoo(p: Int, a: () -> Unit) {}

fun async(q: Int, a: () -> Unit) {}

fun test() {
    asy<caret>Foo(5) {  }
}

// ELEMENT: async
// CHAR: '\t'