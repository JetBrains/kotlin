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

// 2 ATHROW
