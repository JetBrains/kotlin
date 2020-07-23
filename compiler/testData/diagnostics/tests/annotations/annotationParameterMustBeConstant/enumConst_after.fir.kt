// !LANGUAGE: +ProhibitNonConstValuesAsVarargsInAnnotations

annotation class AnnE(val i: MyEnum)

@AnnE(e)
class Test

val e: MyEnum = MyEnum.A

enum class MyEnum {
    A
}

<!INAPPLICABLE_CANDIDATE!>@AnnE(Test())<!>
class Test2
