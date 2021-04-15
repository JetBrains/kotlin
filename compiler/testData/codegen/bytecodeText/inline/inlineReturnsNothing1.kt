// NB '!!' uses Intrinsics.throwNpe/checkNotNull, but IR follows it with ATHROW
// while the old backend returns null from `exit`
inline fun exit(): Nothing = null!!

fun box(): String {
    val a: String
    try {
        a = "OK"
    }
    catch (e: Exception) {
        exit()
        // ATHROW
    }
    return a
}

// JVM_TEMPLATES
// 1 ATHROW
// JVM_IR_TEMPLATES
// 2 ATHROW
