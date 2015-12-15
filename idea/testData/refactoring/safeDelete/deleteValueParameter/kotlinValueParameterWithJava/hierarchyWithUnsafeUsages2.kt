interface Z {
    open fun foo(a: Int, b: Int)
}

open class A {
    open fun foo(<caret>a: Int, b: Int) {
        println(a)
    }
}

open class B: A(), Z {
    override fun foo(a: Int, b: Int) {

    }
}

class C: A() {
    override fun foo(a: Int, b: Int) {

    }
}

class D: B(), Z {
    override fun foo(a: Int, b: Int) {

    }
}