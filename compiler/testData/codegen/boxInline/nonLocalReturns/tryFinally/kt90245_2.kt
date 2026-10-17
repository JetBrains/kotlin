import kotlin.test.assertEquals

var log = ""

inline fun foo(returnFromFinally: Boolean, block: () -> Unit) {
    try {
        try {
            block()
        } finally {
            run {
                if (returnFromFinally) return
            }
            log += "fooInnerFinally;"
        }
    } finally {
        log += "fooOuterFinally;"
    }
}

fun runFoo(returnFromFinally: Boolean): String {
    foo(returnFromFinally) {
        log += "block;"
        return "OK"
    }
    return "afterFooCall"
}

fun box(): String {
    assertEquals("OK", runFoo(false))
    assertEquals("block;fooInnerFinally;fooOuterFinally;", log)

    log = ""
    assertEquals("afterFooCall", runFoo(true))
    assertEquals("block;fooOuterFinally;", log)

    return "OK"
}
