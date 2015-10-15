// KT-4434 Missed diagnostic about else branch in when

package test

fun foo(): Int {
    val a = "a"
    return if (a.length > 0) {
        <!NO_ELSE_IN_WHEN!>when<!> (a) {
            "a" -> 1
        }
    }
    else {
        3
    }
}

fun bar(): Int {
    val a = "a"
    if (a.length > 0) {
        return <!NO_ELSE_IN_WHEN!>when<!> (a) {
            "a" -> 1
        }
    }
    else {
        return 3
    }
}
