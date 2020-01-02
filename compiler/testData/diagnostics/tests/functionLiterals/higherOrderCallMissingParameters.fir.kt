// See KT-7813: Call to functional parameter with missing argument: no error detected but compiler crashes

fun foo(p: (Int, () -> Int) -> Unit) {
    // Errors except last call
    <!INAPPLICABLE_CANDIDATE!>p<!> { 1 }
    <!INAPPLICABLE_CANDIDATE!>p<!>() { 2 }
    p(3) { 4 }
}

fun bar(p: (String, Any, () -> String) -> Unit) {
    // Errors except last call
    <!INAPPLICABLE_CANDIDATE!>p<!> { "" }
    <!INAPPLICABLE_CANDIDATE!>p<!>() { "x" }
    <!INAPPLICABLE_CANDIDATE!>p<!>("y") { "z" }
    p("v", Any()) { "w" }
}