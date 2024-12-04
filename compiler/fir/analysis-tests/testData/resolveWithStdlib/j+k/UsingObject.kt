// RUN_PIPELINE_TILL: BACKEND
class A

fun foo(): A? = null

fun main() {
    val w = foo() ?: java.lang.Object()
    w.hashCode()
}
