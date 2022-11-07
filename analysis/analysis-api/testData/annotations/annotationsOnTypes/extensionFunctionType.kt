inline fun <T> myInit(e: T, init : T.() <caret>-> Unit): T {
    e.init()
    return e
}

fun foo() {
    val initialized = myInit("hello") {
        it + ", DSL"
    }
}