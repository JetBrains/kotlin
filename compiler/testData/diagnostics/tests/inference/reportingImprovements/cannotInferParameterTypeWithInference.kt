package aa

fun <T, R> foo(block: (T)-> R) = block

fun test1() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> {
        <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->  // here we have 'cannot infer parameter type' error
        43
    }
}

fun bar(f: (<!UNRESOLVED_REFERENCE!>A<!>)->Unit) {}

fun test2() {
    bar { <!CANNOT_INFER_PARAMETER_TYPE!>a<!> -> } // here we don't have 'cannot infer parameter type' error
}
