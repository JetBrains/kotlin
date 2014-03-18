class Foo {
    fun get(paramFirst: Int, paramSecond: Int): Int = 42
}

fun test() = Foo()[param<caret>]

// todo - fix
// ABSENT: paramFirst
// ABSENT: paramSecond