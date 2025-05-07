class A<T> {}

fun <T> A<T>.foo(x: T) {}

val r<caret>ef = A<*>::foo
