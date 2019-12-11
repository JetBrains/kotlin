// !LANGUAGE: -ProhibitNonConstValuesAsVarargsInAnnotations

annotation class AnnE(val i: MyEnum)

@AnnE(e)
class Test

val e: MyEnum = MyEnum.A

enum class MyEnum {
    A
}

@AnnE(Test())
class Test2