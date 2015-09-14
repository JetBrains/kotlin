import kotlin.jvm.JvmStatic

enum class A {
    ;
    companion object {
        val foo: String = "OK"

        @JvmStatic val bar: String = "OK"

        @JvmStatic fun baz() = foo
    }
}

fun box(): String {
    if (Test.foo() != "OK") return "Fail foo"
    if (Test.bar() != "OK") return "Fail bar"
    if (Test.getBar() != "OK") return "Fail getBar"
    if (Test.baz() != "OK") return "Fail baz"
    return "OK"
}
