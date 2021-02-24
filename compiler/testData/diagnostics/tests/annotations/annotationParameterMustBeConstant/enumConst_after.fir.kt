// !LANGUAGE: +ProhibitNonConstValuesAsVarargsInAnnotations

annotation class AnnE(val i: MyEnum)

@AnnE(<!ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST!>e<!>)
class Test

val e: MyEnum = MyEnum.A

enum class MyEnum {
    A
}

<!INAPPLICABLE_CANDIDATE!>@AnnE(Test())<!>
class Test2
