enum class A { V1, V2, V3 }

fun test(a: A) {
    val x: Int
    when (a) {
        A.V1 -> x = 11
        A.V2 -> x = 22
        A.V3 -> x = 33
    }
}

// 1 TABLESWITCH
// 0 LOOKUPSWITCH
// 1 ATHROW
