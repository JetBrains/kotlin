// RUN_PIPELINE_TILL: FRONTEND
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
    var i10: MyEnum = Option1<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    var i20: MyEnum = enumProp<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    var i30: MyEnum = <!ARGUMENT_TYPE_MISMATCH!>stringProp<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>

    receive<MyEnum>(Option1<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    receive<MyEnum>(enumProp<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    receive<MyEnum>(<!ARGUMENT_TYPE_MISMATCH!>stringProp<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
}