// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

class Foo {
    init {
        bar()
    }
}

inline fun bar() {
    sb.append({ "OK" }())
}

fun box(): String {
    Foo()
    return sb.toString()
}
