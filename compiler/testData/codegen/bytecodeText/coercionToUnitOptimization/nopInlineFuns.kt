fun test() {
    val a = inlineFunInt { 1 }
    val b = simpleFunInt { 1 }
    val c = inlineFunVoid { val aa = 1 }
    val d = simpleFunVoid { val aa = 1 }
}

inline fun inlineFunInt(f: () -> Int): Int {
    val a = 1
    return f()
}

inline fun inlineFunVoid(f: () -> Unit): Unit {
    val a = 1
    return f()
}

fun simpleFunInt(f: () -> Int): Int {
    return f()
}

fun simpleFunVoid(f: () -> Unit): Unit {
    return f()
}

// 0 NOP
