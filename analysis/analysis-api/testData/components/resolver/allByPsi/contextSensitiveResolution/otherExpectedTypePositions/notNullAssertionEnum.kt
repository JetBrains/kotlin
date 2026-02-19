// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

import kotlin.reflect.KProperty

enum class MyEnum {
    Option1, Option2, Option3;
    companion object {
        val enumProp: MyEnum = Option1
        val stringProp: String = ""
        fun getOption() = Option1
    }
}

fun <T>receive(e: T) {}
val EnumOptionAlias = MyEnum.Option1

fun testNotNullAssertion() {
    var i10: MyEnum = Option1!!
    var i20: MyEnum = enumProp!!
    var i30: MyEnum = stringProp!!

    receive<MyEnum>(Option1!!)
    receive<MyEnum>(enumProp!!)
    receive<MyEnum>(stringProp!!)
}
