// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    for (i in Int.MAX_VALUE until Int.MAX_VALUE) {
        throw AssertionError("This loop shoud not be executed")
    }
    return "OK"
}
