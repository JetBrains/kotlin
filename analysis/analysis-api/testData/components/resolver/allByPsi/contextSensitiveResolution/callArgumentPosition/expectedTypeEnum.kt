// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    EnumValue1;
}

fun <X> id(x: X): X = x
fun <T> receive(arg: T) {}

fun testExpectedType() {
    val r1: MyEnum = id(EnumValue1)

    receive<MyEnum>(id(EnumValue1))
}
