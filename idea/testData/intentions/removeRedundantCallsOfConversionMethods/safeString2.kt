// WITH_RUNTIME
data class Foo(val name: String)

fun test(foo: Foo?) {
    val s: String? = foo?.name?.toString()<caret>
}