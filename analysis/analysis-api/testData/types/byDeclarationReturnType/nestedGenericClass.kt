class A<T> { class B<U> }

fun foo() {
    val r<caret>ef = A.B<Int>()
}
