// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
fun foo(x: java.io.Serializable) {}

fun main() {
    foo("")
}
