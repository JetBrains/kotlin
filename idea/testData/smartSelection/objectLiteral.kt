interface T {
    fun foo(t: T) = t
}

fun foo() {
    object : T {
        val x = 1
    }.foo(<caret>object : T {
        val x = 2
    })
}

/*
object : T {...}
object : T {...}.foo(...)
*/