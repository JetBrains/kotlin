interface A {
    override fun toString(): String {
        return "OK"
    }
}

interface B : A

class C : B {
    override fun toString(): String =
            super.toString()
}
