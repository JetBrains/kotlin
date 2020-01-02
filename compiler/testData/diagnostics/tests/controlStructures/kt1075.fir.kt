// !WITH_NEW_INFERENCE
package kt1075

//KT-1075 No type check for 'in range' condition in 'when' expression

fun foo(b: String) {
    if (b <!INAPPLICABLE_CANDIDATE!>in<!> 1..10) {} //type mismatch
    when (b) {
        <!INAPPLICABLE_CANDIDATE!>in<!> 1..10 -> 1 //no type mismatch, but it should be here
        else -> 2
    }
}