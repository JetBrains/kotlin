// "Change parameter 'a' type of function 'A.times' to 'String'" "true"
trait A {
    fun times(a: String): A
}

fun foo(a: A): A = a * <caret>""