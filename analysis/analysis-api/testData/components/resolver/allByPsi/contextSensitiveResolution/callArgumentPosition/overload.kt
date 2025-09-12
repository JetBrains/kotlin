// ISSUE: KT-75315
// WITH_STDLIB
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

open class MyClass {
    object InheritorObject : MyClass()
}

enum class MyEnum {
    EnumValue1
}
fun overload(arg: MyEnum)   = arg
fun overload(arg: MyClass) = arg
fun test() {
    overload(EnumValue1)

    var v1: MyEnum = overload(EnumValue1)

    overload(InheritorObject)

}
fun overload2(arg: MyEnum) { println("Enum version") }
fun overload2() { println("Without arguments version") }

fun test1() {
    overload2(EnumValue1)
}
fun overload3(arg: MyEnum) { println("Enum version") }
fun overload3(arg: MyEnum, s: String = "") { println("With 2 arguments") }

fun test2() {
    overload3(EnumValue1)
}
