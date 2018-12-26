// IGNORE_BACKEND: JVM_IR

fun test(i: Int): Char {
    val c: Char
    when (i) {
        1 -> c = '1'
        2 -> c = '2'
        3 -> c = '3'
        4 -> c = '4'
        5 -> c = '5'
        6 -> c = '6'
        7 -> c = '7'
        8 -> c = '8'
        9 -> c = '9'
        0 -> c = '0'
        else -> c = ' '
    }
    return c
}

// 12 ISTORE 1
// 1 LOCALVARIABLE c C L1 L16 1
