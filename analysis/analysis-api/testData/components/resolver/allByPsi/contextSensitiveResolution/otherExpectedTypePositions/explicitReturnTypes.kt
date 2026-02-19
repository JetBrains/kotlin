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

val EnumOptionAlias = MyEnum.Option1

fun fun1(): MyEnum = Option1
fun fun2(): MyEnum = enumProp
fun fun3(): MyEnum = stringProp

val propWithGetterType
    get(): MyEnum = Option1

val wrongPropWithGetterType
    get(): MyEnum = stringProp

fun fun4(b: Boolean): MyEnum {
    if (b) return Option1
    return Option2
}
