// !WITH_NEW_INFERENCE
package kt1075

//KT-1075 No type check for 'in range' condition in 'when' expression

fun foo(b: String) {
    if (<!ARGUMENT_TYPE_MISMATCH!>b<!> in 1..10) {} //type mismatch
    when (b) {
        <!ARGUMENT_TYPE_MISMATCH!>in 1..10<!> -> 1 //no type mismatch, but it should be here
        else -> 2
    }
}
