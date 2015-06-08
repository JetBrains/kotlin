open class <caret>A

class B: A() {}

class MyClass(a: A = run { class X: A(); object: A() {} }) {
    inner class G: A() {}

    init {
        class C: A()
    }

    fun foo(a: A = run { class X: A(); object: A() {} }) {
        val t = object {
            inner class F: A()
        }

        class D: A()
    }
}

class MyClass2 {
    constructor(a: A = run { class X: A(); object: A() {} })
}

val bar: Int
    get() {
        class E: A()

        return 0
    }