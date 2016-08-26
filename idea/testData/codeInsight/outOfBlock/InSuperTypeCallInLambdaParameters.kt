// TRUE
// Can't result to false as there's no body expression, so it's considered to be changes in JavaCodeBlockModificationListener.

open class A(a: () -> Unit) {
    constructor(f: (String) -> Unit) : this({ -> f("") })
}

class B: A({ s<caret> -> "1" })

// SKIP_ANALYZE_CHECK