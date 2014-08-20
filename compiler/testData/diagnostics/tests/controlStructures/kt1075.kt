package kt1075

//KT-1075 No type check for 'in range' condition in 'when' expression

fun foo(b: String) {
    if (<!TYPE_MISMATCH!>b<!> in 1..10) {} //type mismatch
    when (b) {
        <!TYPE_MISMATCH_IN_RANGE!>in<!> 1..10 -> <!UNUSED_EXPRESSION!>1<!> //no type mismatch, but it should be here
        else -> <!UNUSED_EXPRESSION!>2<!>
    }
}