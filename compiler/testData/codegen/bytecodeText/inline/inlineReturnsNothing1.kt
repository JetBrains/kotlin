// NB '!!' uses Intrinsics.throwNpe/checkNotNull
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

// 1 ATHROW
