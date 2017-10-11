package runtime.memory.throw_cleanup

import kotlin.test.*

@Test fun runTest() {
    foo(false)
    try {
        foo(true)
    } catch (e: Error) {
        println("Ok")
    }
}

fun foo(b: Boolean): Any {
    var result = Any()
    if (b) {
        throw Error()
    }
    return result
}