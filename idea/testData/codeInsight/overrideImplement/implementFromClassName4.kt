interface A {
    fun a1()
    fun a2()
    fun a3()
    fun a4()
    fun a5()
    fun a6()
    fun a7()
    fun a8()
}

class <caret>Test : A {
    override fun a4() {
    }

    override fun a6() {
    }

    override fun a2() {
    }
}