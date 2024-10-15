// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A : Function0<Int> {
    override fun invoke(): Int = 1
}

fun main() {
    A()()
}