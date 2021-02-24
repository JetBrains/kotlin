// !DUMP_CFG

annotation class Ann

fun foo(b: Boolean) {
    if (b) {
        return
    }

    @Ann
    class Local()

    bar()
}

fun bar() {}
