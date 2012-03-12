//KT-234 Force when() expressions to have an 'else' branch
//KT-973 Unreachable code

package kt234_kt973


fun test(t : #(Int, Int)) : Int {
    <!NO_ELSE_IN_WHEN!>when<!> (t) {
        is #(10, 10) -> return 1
    }
    return 0 // unreachable code
}

fun test1(t : #(Int, Int)) : Int {
    when (t) {
        is #(10, 10) -> return 1
        else -> return 2
    }
    <!UNREACHABLE_CODE!>return 0<!> // unreachable code
}

//more tests
fun t1(x: Int) = when(x) {
    is * -> 1
}

fun t2(x: Int) = when(x) {
    is val a -> 1
}

fun t3(x: Int) = when (x) {
    is * -> 1
    <!UNREACHABLE_CODE!>2 -> 2<!>
}

fun t4(x: Int) = when (x) {
    is val a -> 1
    <!UNREACHABLE_CODE!>2 -> 2<!>
}

fun t5(x: Int) = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is val a : Int -> 1
    2 -> 2
}

fun foo3(x: Int) = when(x) {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> 1
    <!UNREACHABLE_CODE!>2 -> 2<!>
}

fun foo4(x: Int) = when(x) {
    2 -> x
    is val a -> a
}
