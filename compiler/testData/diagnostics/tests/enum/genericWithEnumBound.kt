// ISSUE: KT-59333

interface MyInterface {
    val value: String
}

enum class WrongTypeEnum {
    WrongEnum1,
    WrongEnum2,
}

enum class CorrectTypeEnum(override val value: String) : MyInterface {
    CorrectEnum1("1"),
    CorrectEnum2("2"),
}

fun <T : Enum<out MyInterface>> myFunction(arg: T) {}

class MyClass<T : Enum<out <!UPPER_BOUND_VIOLATED!>MyInterface<!>>>(val arg: T)

fun test() {
    myFunction(CorrectTypeEnum.CorrectEnum1)
    myFunction(<!TYPE_MISMATCH!>WrongTypeEnum.WrongEnum1<!>)
    MyClass(CorrectTypeEnum.CorrectEnum1)
    MyClass(<!TYPE_MISMATCH!>WrongTypeEnum.WrongEnum1<!>)
}
