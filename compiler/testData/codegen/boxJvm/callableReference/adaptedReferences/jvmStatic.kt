// WITH_STDLIB
// TARGET_BACKEND: JVM
object Test {
    @JvmStatic
    fun foo(x: String, y: String = "") = x + value

    var value = ""
}

fun callFoo(f: (String) -> String, value: String) = f(value)

fun test() = Test.apply { value = "K" }

fun box() = callFoo(Test::foo, "O") + callFoo(test()::foo, "")
