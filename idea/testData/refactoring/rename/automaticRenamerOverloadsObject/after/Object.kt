package testing

object Object {
    fun bar() {
        "".foo()
    }

    fun bar(a: Int) {
    }

    fun String.foo() {
    }

}

fun main(args: Array<String>) {
    Object.bar()
    Object.bar(1)
}