fun foo(x: Int): String {
    return when (val y = x) {
        100 -> "1"
        200 -> "2"
        300 -> "3"
        else -> "else"
    }
}

// 1 LOOKUPSWITCH
// 0 TABLESWITCH