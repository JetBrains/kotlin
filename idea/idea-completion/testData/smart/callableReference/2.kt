fun foo(p: (Int) -> Unit){}

fun bar() {
    foo(<caret>)
}

fun f(){}
fun f(i: Int){}

// EXIST: ::f
