class Test1 : Enum<Test1>("", 0)

class Outer {
    class Test2 : Enum<Test2>("", 0)
}

fun outer() {
    class Test3 : Enum<Test3>("", 0)
}