fun foo1(x: Int): Boolean {
    when(x) {
        2 + 2 -> return true
        else -> return false
    }
}

fun foo2(x: Int): Boolean {
    when(x) {
        Integer.MAX_VALUE -> return true
        else -> return false
    }
}

// 0 TABLESWITCH
// 2 LOOKUPSWITCH
