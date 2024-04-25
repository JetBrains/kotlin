// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

context(Int, String)
fun foo(): String = "O"

context(Int)
fun foo(): String = "K"

fun box(): String {
    val o = with ("") {
        with(42) {
            foo()
        }
    }
    val k = with(42) {
        foo()
    }
    return o + k
}
