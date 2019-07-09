fun foo(a: Int, b: Int) = if (a > b) a else b

fun bar(a: Double, b: Double): Double {
    if (a > b) {
        println(a)
        return a
    } else {
        println(b)
        return b
    }
}

fun baz(a: Long, b: Long): Long {
    when {
        a > b -> {
            println(a)
            return a
        }
        else -> return b
    }
}

fun grade(g: Int): String {
    return when (g) {
        6, 7 -> "Outstanding"
        5 -> "Excellent"
        4 -> "Good"
        3 -> "Mediocre"
        in 1..2 -> "Fail"
        is Number -> "Number"
        else -> "Unknown"
    }
}