fun foo(x: String): String {
    return when (val y = x) {
        "1" -> "one"
        "2" -> "two"
        "3" -> "two and one"
        "4" -> "two and two"
        else -> "many"
    }
}

// 0 LOOKUPSWITCH
// 1 TABLESWITCH