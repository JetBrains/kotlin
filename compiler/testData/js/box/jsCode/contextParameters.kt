// LANGUAGE: +ContextParameters
class Scope {
    fun foo() = js("return 'OK';")
}

context(scope: Scope)
fun test() = scope.foo()

fun box(): String {
    with(Scope()) {
        return test()
    }
}