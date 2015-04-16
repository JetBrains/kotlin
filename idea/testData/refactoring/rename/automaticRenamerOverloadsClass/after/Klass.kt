package testing

open class Klass {
    fun bar() {
        "".bar()
    }

    open fun bar(a: Int) {
    }

    fun String.bar() {
    }

}

class Sub : Klass() {
    override fun bar(a: Int) {
    }
}

fun main(args: Array<String>) {
    Klass().bar()
    Klass().bar(1)
}