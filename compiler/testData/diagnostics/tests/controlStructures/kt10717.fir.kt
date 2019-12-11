// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNREACHABLE_CODE -UNUSED_PARAMETER -RETURN_NOT_ALLOWED

fun test1() = run {
    return "OK"
}

fun test2() = run {
    fun local(): String {
        return ""
    }
    return ""
}

inline fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> = null!!
fun test3(a: List<String>, b: List<Int>) = a.map {
    if (it.length == 3) return null
    if (it.length == 4) return ""
    if (it.length == 4) return 5
    if (it.length == 4) return b
    1
}

fun test4() = run {
    fun test5() {
        return

        return@test4

        return return@test4

        return fun() { return; return@test4 "" }
    }

    return
    3
}

val foo: Int
    get() = run {
        if (true) return ""

        return
    }

fun test(): Int = run {
    return ""
}
