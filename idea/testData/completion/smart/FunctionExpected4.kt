fun foo(p: (() -> Unit)?){}

fun bar() {
    foo(<caret>)
}

fun f1(){}
fun f2(i: Int){}

// EXIST: ::f1
// ABSENT: ::f2
