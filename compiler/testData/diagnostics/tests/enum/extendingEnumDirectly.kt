// FIR_IDENTICAL
class Test1 : <!CLASS_CANNOT_BE_EXTENDED_DIRECTLY!>Enum<Test1><!>("", 0)

typealias TA<T> = Enum<T>

class TestTa : <!CLASS_CANNOT_BE_EXTENDED_DIRECTLY!>TA<TestTa><!>("", 0)

class Outer {
    class Test2 : <!CLASS_CANNOT_BE_EXTENDED_DIRECTLY!>Enum<Test2><!>("", 0)
}

fun outer() {
    class Test3 : <!CLASS_CANNOT_BE_EXTENDED_DIRECTLY!>Enum<Test3><!>("", 0)
}