// ISSUE: KT-75316
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    OK
}

sealed class MySealed {
    data object Ok : MySealed()
}

class MyClass {
    companion object {
        val INSTANCE = MyClass()
    }

    override fun toString() = "OK"
}

fun box(): String {
    val t1: MyEnum = OK
    if (t1.name != "OK") return "fail 1"

    val t2: MySealed = Ok
    if (t2.toString() != "Ok") return "fail 2"

    val t3: MyClass = INSTANCE
    if (t3.toString() != "OK") return "fail 3"

    return "OK"
}
