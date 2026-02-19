val (a1, a2) = MyClass()
val (a3, a4) = MyClass().let {
    a1
    it
}

val (a5, a6) = MyClass()

class MyClass {
    operator fun component1() = 1
    operator fun component2() = ""
}

a<caret>4