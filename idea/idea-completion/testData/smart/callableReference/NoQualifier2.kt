fun foo(p: (Int) -> Unit){}

fun bar() {
    foo(<caret>)
}

fun f(){}
fun f(i: Int){}

// currently not supported for generic funcitons
fun<T> genericF(t: T){}

// EXIST: ::f
// ABSENT: ::genericF
