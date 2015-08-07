import kotlin.platform.platformStatic

enum class A {
    ;
    companion object {
        val foo: String = "OK"

        platformStatic val bar: String = "OK"

        platformStatic fun baz() = foo
    }
}

fun box(): String {
    if (Test.foo() != "OK") return "Fail foo"
    if (Test.bar() != "OK") return "Fail bar"
    if (Test.getBar() != "OK") return "Fail getBar"
    if (Test.baz() != "OK") return "Fail baz"
    return "OK"
}
