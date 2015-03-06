package defaultObject

class A {
    default object {
    }
}

class B {
    default object NamedUnused {
    }
}

class C {
    default object NamedUsed {
    }
}

fun main(args: Array<String>) {
    A()
    B()
    C()
    C.NamedUsed
}