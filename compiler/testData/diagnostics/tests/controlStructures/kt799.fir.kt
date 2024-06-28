//KT-799 Allow 'return' expressions in conditionals assigned to variables

package kt799

fun test() {
    val a : Int = if (true) 6 else return // should be allowed

    val b = if (true) 6 else return // should be allowed

    doSmth(if (true) 3 else return)

    doSmth(if (true) 3 else return, <!TOO_MANY_ARGUMENTS!>1<!>)
}

val a : Nothing = <!RETURN_NOT_ALLOWED!>return<!> 1

val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>b<!> = <!RETURN_NOT_ALLOWED!>return<!> 1

val c = doSmth(if (true) 3 else <!RETURN_NOT_ALLOWED!>return<!>)


fun f(mi: Int = if (true) 0 else <!RETURN_NOT_ALLOWED!>return<!>) {}

fun doSmth(i: Int) {
}
