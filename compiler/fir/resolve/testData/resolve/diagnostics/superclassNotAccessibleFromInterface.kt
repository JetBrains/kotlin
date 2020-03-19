class A {
    fun foo() {}
}

fun bar() {}

interface B: A() {
    fun act() {
        super.foo()

        bar()

        fire()
    }

    fun fire() {}
}
