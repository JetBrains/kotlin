// RUN_PIPELINE_TILL: FRONTEND
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
fun fun3(): MyEnum = <!RETURN_TYPE_MISMATCH!>stringProp<!>

val propWithGetterType
    get(): MyEnum = Option1

val wrongPropWithGetterType
    get(): MyEnum = <!RETURN_TYPE_MISMATCH!>stringProp<!>

fun fun4(b: Boolean): MyEnum {
    if (b) return Option1
    return Option2
}