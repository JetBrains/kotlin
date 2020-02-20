enum class A { V1, V2, V3 }

fun box(a: A): String {
    when (a) {
        A.V1 -> return "V1"
        A.V2 -> return "V2"
        A.V3 -> return "V3"
    }
}

// 1 TABLESWITCH
// 0 LOOKUPSWITCH
// 1 ATHROW
