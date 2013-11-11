val s = ""

fun f(p1: Object, p2: String) {
    foo(<caret>)
}

fun foo(p1: String, p2: Object) : String{
}

// ABSENT: p1
// EXIST: p2
// EXIST: s
// EXIST: foo
