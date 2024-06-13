// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class Context

fun interface SAM {
    context(Context)
    fun foo(x: Int): Int
}

fun box(): String {
    val sam1 = SAM { x -> x + 1 }
    val sam2 = SAM { 2 }
    val sam3 = SAM { it + 1 }

    with(Context()) {
        sam1.foo(0)
        sam2.foo(0)
        sam3.foo(0)
    }

    return "OK"
}