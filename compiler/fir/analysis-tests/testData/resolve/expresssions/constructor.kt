// RUN_PIPELINE_TILL: BACKEND
class C {

    fun create() = C()
}

fun foo() = C()
fun bar() = foo().create()
