fun f(fooBar: String){}

fun g(a: String, bar: String, fooBar: String) {
    f(<caret>)
}

// ORDER: fooBar, bar, a
