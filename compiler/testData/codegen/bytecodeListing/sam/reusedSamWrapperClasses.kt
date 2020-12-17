// FILE: test.kt
fun test1() {
    val f = { }
    val t1 = Runnable(f)
    val t2 = Runnable(f)
}

fun test2() {
    val t1 = getWrapped1()
    val t2 = getWrapped2()
}

// FILE: f1.kt
fun getWrapped1(): Runnable {
    val f = { }
    return Runnable(f)
}

// FILE: f2.kt
fun getWrapped2(): Runnable {
    val f = { }
    return Runnable(f)
}

