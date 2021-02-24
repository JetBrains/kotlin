// FIR_IDENTICAL
annotation class AnnE(val i: String)

enum class MyEnum {
    A
}

@AnnE(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"1" + MyEnum.A<!>)
class Test

@AnnE(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"1" + MyEnum::class<!>)
class Test2

@AnnE(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"1" + AnnE("23")<!>)
class Test3

@AnnE(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"1" + arrayOf("23", "34")<!>)
class Test4

