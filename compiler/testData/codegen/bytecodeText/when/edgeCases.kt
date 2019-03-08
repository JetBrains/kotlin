fun foo(x: Int): String {
    return when (x) {
        2_147_483_647 -> "MAX"
        -2_147_483_648 -> "MIN"
        else -> "else"
    }
}

// 1 LOOKUPSWITCH
// 0 TABLESWITCH