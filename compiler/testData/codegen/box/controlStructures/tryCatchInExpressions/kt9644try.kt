// IGNORE_BACKEND: JVM_IR
inline fun doCall(f: () -> Any) = f()

fun test1() {
    val localResult = doCall {
        try { "1" } catch (e: Exception) { "2" }
        return
    }
}

fun test2(): String {
    val localResult = doCall {
        try { "1" } catch (e: Exception) { "2" }
        return@test2 "OK"
    }
    return "Hmmm..."
}

fun box(): String {
    test1()
    return test2()
}