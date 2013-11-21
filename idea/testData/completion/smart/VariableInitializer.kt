val foo = ""

fun f(p1: Any, p2: String) {
    var a : String = <caret>
}

// ABSENT: p1
// EXIST: p2
// EXIST: foo
// ABSENT: a
