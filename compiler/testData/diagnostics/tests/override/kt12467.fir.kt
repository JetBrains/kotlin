interface A {
    fun test() {
    }
}

interface B : A {
    override fun test()
}

interface C : A

interface D : C, B

class K : D
