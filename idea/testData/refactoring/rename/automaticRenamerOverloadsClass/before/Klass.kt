package testing

open class Klass {
    fun foo() {
        "".foo()
    }

    open fun foo(a: Int) {
    }

    fun String.foo() {
    }

}

fun Klass.foo(a: Int, b: Int) {
}

class Sub : Klass() {
    override fun foo(a: Int) {
    }
}

fun main(args: Array<String>) {
    Klass().foo()
    Klass().foo(1)
}