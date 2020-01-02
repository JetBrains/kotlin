annotation class AnnE(val i: String)

enum class MyEnum {
    A
}

@AnnE("1" + MyEnum.A)
class Test

@AnnE("1" + MyEnum::class)
class Test2

@AnnE("1" + AnnE("23"))
class Test3

@AnnE("1" + arrayOf("23", "34"))
class Test4

