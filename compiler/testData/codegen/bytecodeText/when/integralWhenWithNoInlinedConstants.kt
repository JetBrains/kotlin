fun foo1(x: Int): Boolean {
    when(x) {
        0 -> return true
        1 -> return false
        2 -> return true
        3 -> return false
        2 + 2 -> return true
        else -> return false
    }
}

fun foo2(x: Int): Boolean {
    when(x) {
        0 -> return true
        1 -> return false
        2 -> return true
        3 -> return false
        Integer.MAX_VALUE -> return true
        else -> return false
    }
}

// 1 TABLESWITCH
// 1 LOOKUPSWITCH
