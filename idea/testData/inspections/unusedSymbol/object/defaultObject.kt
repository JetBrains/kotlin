package defaultObject

class A {
    class object {
    }
}

class B {
    class object NamedUnused {
    }
}

class C {
    class object NamedUsed {
    }
}

fun main(args: Array<String>) {
    A()
    B()
    C()
    C.NamedUsed
}