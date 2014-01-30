annotation class AnnE(val i: MyEnum)

AnnE(<!ANNOTATION_PARAMETER_MUST_BE_ENUM_CONST!>e<!>)
class Test

val e: MyEnum = MyEnum.A

enum class MyEnum {
    A
}

AnnE(<!TYPE_MISMATCH!>Test()<!>)
class Test2