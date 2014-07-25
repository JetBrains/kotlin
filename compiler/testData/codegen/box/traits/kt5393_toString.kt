interface A {
    override fun toString(): String {
        return "OK"
    }
}

interface B : A

class C : B {
    override fun toString(): String {
        return super.toString()
    }
}

fun box() = "${C()}"
