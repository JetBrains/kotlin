package foo


inline fun <T> buzz(x: T): T {
    log("buzz($x)")
    return x
}

// CHECK_NOT_CALLED: buzz

private var LOG = ""

fun log(string: String) {
    LOG += "$string;"
}

fun pullLog(): String {
    val string = LOG
    LOG = ""
    return string
}

fun <T> fizz(x: T): T {
    log("fizz($x)")
    return x
}

object A {
    init {
        log("A.init")
    }

    val x = 23
}

inline fun bar(value: Int) {
    log("bar->begin")
    log("value=$value")
    log("bar->end")
}

fun box(): String {
    bar(A.x)
    assertEquals("A.init;bar->begin;value=23;bar->end;", pullLog())
    return "OK"
}