class C {

    fun create() = C()
}

fun foo() = C()
fun bar() = foo().create()