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

// 4 ATHROW