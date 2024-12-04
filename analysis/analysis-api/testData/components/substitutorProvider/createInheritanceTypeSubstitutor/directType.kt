// WITH_STDLIB

class A<caret_base>A : BB<Int>()

open class B<caret_super>B<S> {
    fun foo(): List<S> = TODO()
}

