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
    return f() // return replaced with nop to stop here *after* calling f
}

fun simpleFunInt(f: () -> Int): Int {
    return f()
}

fun simpleFunVoid(f: () -> Unit): Unit {
    return f() // return replaced with nop to stop here *after* calling f
}

// 2 NOP
