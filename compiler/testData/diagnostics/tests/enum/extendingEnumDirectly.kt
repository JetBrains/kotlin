class Test1 : <!CLASS_CANNOT_BE_EXTENDED_DIRECTLY!>Enum<Test1><!>("", 0)

class Outer {
    class Test2 : <!CLASS_CANNOT_BE_EXTENDED_DIRECTLY!>Enum<Test2><!>("", 0)
}

fun outer() {
    class Test3 : <!CLASS_CANNOT_BE_EXTENDED_DIRECTLY!>Enum<Test3><!>("", 0)
}