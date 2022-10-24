// See KT-7813: Call to functional parameter with missing argument: no error detected but compiler crashes

fun foo(p: (Int, () -> Int) -> Unit) {
    // Errors except last call
    <!NO_VALUE_FOR_PARAMETER!>p<!> { 1 }
    <!NO_VALUE_FOR_PARAMETER!>p()<!> { 2 }
    p(3) { 4 }
}

fun bar(p: (String, Any, () -> String) -> Unit) {
    // Errors except last call
    <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>p<!> { "" }
    <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>p()<!> { "x" }
    p(<!NO_VALUE_FOR_PARAMETER!>"y")<!> { "z" }
    p("v", Any()) { "w" }
}
