// IGNORE_BACKEND: JVM

fun test(x: Int): String {
    return when {
        x == 1 || x == 3 || x == 5 -> "135"
        x == 2 || x == 4 || x == 6 -> "246"
        else -> "other"
    }
}

// 1 TABLESWITCH
