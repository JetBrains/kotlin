// KT-34027
expect interface A<T> {
    fun foo(x: T)
}

fun bar(): A<String> = null!!
