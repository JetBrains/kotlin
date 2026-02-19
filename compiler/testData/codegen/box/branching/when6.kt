// WITH_STDLIB

import kotlin.test.*

fun foo() {
}

fun box(): String {
    if (true) foo() else foo()

    return "OK"
}
