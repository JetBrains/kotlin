// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

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

fun testElvis() {
    var i10: MyEnum = Option1 ?: Option2
    var i11: MyEnum? = Option1 ?: Option2
    var i20: MyEnum = enumProp ?: Option2
    var i30: MyEnum = enumProp ?: stringProp
    var i40: MyEnum = stringProp ?: enumProp
    var i50: MyEnum = getOption() ?: enumProp

    receive<MyEnum>(Option1 ?: Option2)
    receive<MyEnum>(enumProp ?: stringProp)
    receive<MyEnum>(enumProp ?: getOption())
}
