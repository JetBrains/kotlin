package aa

fun <T, R> foo(block: (T)-> R) = block

fun test1() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> {
        <!CANNOT_INFER_PARAMETER_TYPE, UNUSED_PARAMETER!>x<!> ->  // here we have 'cannot infer parameter type' error
        43
    }
}

fun bar(<!UNUSED_PARAMETER!>f<!>: (<!UNRESOLVED_REFERENCE!>A<!>)->Unit) {}

fun test2() {
    bar { <!UNUSED_PARAMETER!>a<!> -> } // here we don't have 'cannot infer parameter type' error
}
