enum class TestEnum {
    Foo
}

annotation class Ann(vararg val a: TestEnum)

val foo = TestEnum.Foo
var bar = TestEnum.Foo

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST!>foo<!>, <!ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST!>bar<!>)
fun test() {}