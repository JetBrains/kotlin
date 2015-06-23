package testing

open class Klass {
    fun bar() {
        "".foo()
    }

    open fun bar(a: Int) {
    }

    fun String.foo() {
    }

}

fun Klass.bar(a: Int, b: Int) {
}

class Sub : Klass() {
    override fun bar(a: Int) {
    }
}

fun main(args: Array<String>) {
    Klass().bar()
    Klass().bar(1)
}