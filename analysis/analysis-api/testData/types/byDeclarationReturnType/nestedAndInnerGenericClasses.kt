class A<AA> {
    class B<BB> {
        inner class C<CC>
    }
}

fun fo<caret>o(): A.B<String>.C<Int>? = null
