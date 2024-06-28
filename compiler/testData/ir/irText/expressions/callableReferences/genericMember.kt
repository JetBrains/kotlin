// FIR_IDENTICAL

class A<T> {
    fun foo() {}
    val bar = 42
}

val test1 = A<String>::foo
val test2 = A<String>::bar
