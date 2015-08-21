import kotlin.jvm.jvmStatic

enum class A {
    ;
    companion object {
        val foo: String = "OK"

        jvmStatic val bar: String = "OK"

        jvmStatic fun baz() = foo
    }
}

fun box(): String {
    if (Test.foo() != "OK") return "Fail foo"
    if (Test.bar() != "OK") return "Fail bar"
    if (Test.getBar() != "OK") return "Fail getBar"
    if (Test.baz() != "OK") return "Fail baz"
    return "OK"
}
