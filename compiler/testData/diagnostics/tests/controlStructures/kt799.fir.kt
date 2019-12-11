//KT-799 Allow 'return' expressions in conditionals assigned to variables

package kt799

fun test() {
    val a : Int = if (true) 6 else return // should be allowed

    val b = if (true) 6 else return // should be allowed

    doSmth(if (true) 3 else return)

    <!INAPPLICABLE_CANDIDATE!>doSmth<!>(if (true) 3 else return, 1)
}

val a : Nothing = return 1

val b = return 1

val c = doSmth(if (true) 3 else return)


fun f(mi: Int = if (true) 0 else return) {}

fun doSmth(i: Int) {
}
