//KT-2146 Nullability casts in when.
package kt2146

fun f1(s: Int?): Int {
    return when (s) {
        null -> 3
        else -> s
    }
}

fun f2(s: Int?): Int {
    return when (s) {
        !is Int -> <!TYPE_MISMATCH!>s<!>
        else -> s
    }
}

fun f3(s: Int?): Int {
    return when (s) {
        is Int -> s
        else -> <!TYPE_MISMATCH!>s<!>
    }
}

fun f4(s: Int?): Int {
    return when {
        s == 4 -> s
        s == null -> <!TYPE_MISMATCH!>s<!>
        else -> s
    }
}

fun f5(s: Int?): Int {
    return when (s) {
        s!! -> s
        s -> <!TYPE_MISMATCH!>s<!>
        else -> 0
    }
}

fun f6(s: Int?): Int {
    return when {
        s is Int -> s
        else -> <!TYPE_MISMATCH!>s<!>
    }
}

fun f7(s: Int?): Int {
    return when {
        s !is Int -> <!TYPE_MISMATCH!>s<!>
        else -> s
    }
}
