abstract class A {
    abstract override fun toString(): String
}

interface B

abstract class C : A(), B

class Test : C()
