open class A
class B : A()

private fun processB(b: B): Int = 2

fun test(a: A) {
    if (a is B) {
        <expr>processB(a)</expr>
    }
}
