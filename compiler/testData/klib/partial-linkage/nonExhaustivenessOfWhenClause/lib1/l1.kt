sealed interface SI1 {
    class A : SI1
    class B : SI1
}

sealed class SC1 {
    class A: SC1()
    class B: SC1()
}

enum class E1 {
    A, B
}

enum class E2 {
    A, B
}

sealed interface SI2 {
    class ClassToObject : SI2
    object ObjectToClass : SI2
}

sealed class SC2 {
    class ClassToObject : SC2()
    object ObjectToClass : SC2()
}
