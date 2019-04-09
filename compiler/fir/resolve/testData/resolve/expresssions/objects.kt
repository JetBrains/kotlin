object A {
    fun foo() = this
}

fun use() = A
fun bar() = A.foo()