//KT-2146 Nullability casts in when.
package kt2146

fun f1(s: Int?): Int {
    return when (s) {
        null -> 3
        else -> s
    }
}

fun f2(s: Int?): Int {
    return <!RETURN_TYPE_MISMATCH!>when (s) {
        !is Int -> s
        else -> s
    }<!>
}

fun f3(s: Int?): Int {
    return <!RETURN_TYPE_MISMATCH!>when (s) {
        is Int -> s
        else -> s
    }<!>
}

fun f4(s: Int?): Int {
    return <!RETURN_TYPE_MISMATCH!>when {
        s == 4 -> s
        s == null -> s
        else -> s
    }<!>
}

fun f5(s: Int?): Int {
    return <!RETURN_TYPE_MISMATCH!>when (s) {
        s -> s
        s!! -> s
        s -> s
        else -> 0
    }<!>
}

fun f6(s: Int?): Int {
    return <!RETURN_TYPE_MISMATCH!>when {
        s is Int -> s
        else -> s
    }<!>
}

fun f7(s: Int?): Int {
    return <!RETURN_TYPE_MISMATCH!>when {
        s !is Int -> s
        else -> s
    }<!>
}
