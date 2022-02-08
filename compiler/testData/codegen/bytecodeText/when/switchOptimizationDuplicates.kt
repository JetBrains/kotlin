fun foo(x: Int): Int {
    return when (x) {
        1, 1, 2 -> 1001
        1, 2 -> 1002
        1 -> 1003
        2 -> 1004
        3 -> 1005
        else -> 1006
    }
}

// 1 SIPUSH 1001
// 0 SIPUSH 1002
// 0 SIPUSH 1003
// 0 SIPUSH 1004
// 1 SIPUSH 1005
// 1 SIPUSH 1006
// 1 TABLESWITCH
// 0 LOOKUPSWITCH