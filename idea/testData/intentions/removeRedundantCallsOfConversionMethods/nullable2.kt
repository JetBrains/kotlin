// IS_APPLICABLE: false
// WITH_RUNTIME
data class Foo(val name: String)

fun nullable2(foo: Foo?) {
    val s: String = foo?.name.toString()<caret>
}