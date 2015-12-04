enum class A { V }

fun box(): String {
    val a: A = A.V
    when (a) {
        A.V -> return "OK"
    }
}

// 0 TABLESWITCH
// 1 LOOKUPSWITCH
// 1 ATHROW
