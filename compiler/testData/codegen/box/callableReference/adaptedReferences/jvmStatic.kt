// WITH_RUNTIME
// TARGET_BACKEND: JVM
object Test {
    @JvmStatic
    fun foo(x: String, y: String = "") = x + y
}

fun callFoo(f: (String) -> String, value: String) = f(value)

fun test() = Test

fun box() = callFoo(Test::foo, "O") + callFoo(test()::foo, "K")
