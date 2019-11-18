// IGNORE_BACKEND_FIR: JVM_IR
fun printlnMock(a: Any) {}

public fun testCoalesce() {
    val value: String = when {
        true -> {
            if (true) {
                "foo"
            } else {
                "bar"
            }
        }
        else -> "Hello world"
    }

    printlnMock(value.length)
}

fun box(): String {
    testCoalesce()
    return "OK"
}
