// WITH_STDLIB
class A<caret_base>A : BB<Int, String>()

open class B<caret_super>B<S, Q, U> {
    fun foo(): Triple<S, Q, U> = TODO()
}

