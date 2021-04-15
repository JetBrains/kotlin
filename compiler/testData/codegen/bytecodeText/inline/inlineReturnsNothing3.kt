// NB '!!' uses Intrinsics.throwNpe/checkNotNull, but IR follows it with ATHROW
// while the old backend returns null from `exit`
inline fun exit(): Nothing = null!!
inline fun exita(): Nothing = exit() // ATHROW
inline fun exitb(): Nothing = exita() // ATHROW
inline fun exitc(): Nothing = exitb() // ATHROW

fun box(): String {
    val a: String
    try {
        a = "OK"
    }
    catch (e: Exception) {
        exitc() // ATHROW
    }
    return a
}

// JVM_TEMPLATES
// 4 ATHROW
// JVM_IR_TEMPLATES
// 5 ATHROW
