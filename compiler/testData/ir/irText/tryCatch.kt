fun test1() {
    try {
        println()
    }
    catch (e: Throwable) {
        println()
    }
    finally {
        println()
    }
}

fun test2(): Int {
    return try {
        println()
        42
    }
    catch (e: Throwable) {
        println()
        24
    }
    finally {
        println()
    }
}

fun testImplicitCast(a: Any) {
    if (a !is String) return

    val t: String = try { a } catch (e: Throwable) { "" }
}