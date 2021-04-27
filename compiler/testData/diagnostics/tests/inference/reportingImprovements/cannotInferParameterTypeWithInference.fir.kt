// !WITH_NEW_INFERENCE
package aa

fun <T, R> foo(block: (T)-> R) = block

fun test1() {
    foo {
        x ->  // here we have 'cannot infer parameter type' error
        <!ARGUMENT_TYPE_MISMATCH!>43<!>
    }
}

fun bar(f: (<!UNRESOLVED_REFERENCE!>A<!>)->Unit) {}

fun test2() {
    bar { <!UNRESOLVED_REFERENCE!>a<!> -> } // here we don't have 'cannot infer parameter type' error
}
