// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNREACHABLE_CODE -UNUSED_PARAMETER -RETURN_NOT_ALLOWED

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>test1<!>() = run {
    return <!TYPE_MISMATCH("String", "Nothing")!>"OK"<!>
}

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>test2<!>() = run {
    fun local(): String {
        return ""
    }
    return <!TYPE_MISMATCH("String", "Nothing")!>""<!>
}

inline fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> = null!!
fun test3(a: List<String>, b: List<Int>) = a.map {
    if (it.length == 3) return <!TYPE_MISMATCH("Nothing?", "List<Int>")!>null<!>
    if (it.length == 4) return <!TYPE_MISMATCH("String", "List<Int>")!>""<!>
    if (it.length == 4) return <!TYPE_MISMATCH("Int", "List<Int>")!>5<!>
    if (it.length == 4) return b
    1
}

fun test4() = run {
    fun test5() {
        return

        <!RETURN_TYPE_MISMATCH!>return@test4<!>

        return <!RETURN_TYPE_MISMATCH!>return@test4<!>

        return <!TYPE_MISMATCH!>fun() { return; return@test4 <!TYPE_MISMATCH!>""<!> }<!>
    }

    <!RETURN_TYPE_MISMATCH!>return<!>
    3
}

val foo: Int
    get() = run {
        if (true) return <!TYPE_MISMATCH!>""<!>

        <!RETURN_TYPE_MISMATCH!>return<!>
    }

fun test(): Int = run {
    return <!TYPE_MISMATCH!>""<!>
}
