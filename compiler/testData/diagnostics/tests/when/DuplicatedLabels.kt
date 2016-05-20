package test

const val four = 4

fun first(arg: Int) = when (arg) {
    1 -> 2
    2 -> 3
    <!DUPLICATE_LABEL_IN_WHEN!>1<!> -> 4
    4 -> 5
    <!DUPLICATE_LABEL_IN_WHEN!>1<!> -> 6
    <!DUPLICATE_LABEL_IN_WHEN!>2<!> -> 7
    // Error should be here: see KT-11971
    four -> 8
    else -> 0
}

fun second(arg: String): Int {
    when (arg) {
        "ABC" -> return 0
        "DEF" -> return 1
        <!DUPLICATE_LABEL_IN_WHEN!>"ABC"<!> -> return -1
        <!DUPLICATE_LABEL_IN_WHEN!>"DEF"<!> -> return -2
    }
    return 42
}

fun third(arg: Any?): Int {
    when (arg) {
        null -> return -1
        is String -> return 0
        is Double -> return 1
        is <!DUPLICATE_LABEL_IN_WHEN!>Double<!> -> return 2
        <!DUPLICATE_LABEL_IN_WHEN!>null<!> -> return 3
        else -> return 5
    }
}

enum class Color { RED, GREEN, BLUE }

fun fourth(arg: Color) = when (arg) {
    Color.RED -> "RED"
    Color.GREEN -> "GREEN"
    <!DUPLICATE_LABEL_IN_WHEN!>Color.RED<!> -> "BLUE"
    Color.BLUE -> "BLUE"
}

fun fifth(arg: Any?) = when (arg) {
    is Any -> "Any"
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    <!UNREACHABLE_CODE!>else -> null<!>
}
