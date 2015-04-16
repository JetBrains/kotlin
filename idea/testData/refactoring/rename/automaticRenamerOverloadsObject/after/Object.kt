package testing

object Object {
    fun bar() {
        "".bar()
    }

    fun bar(a: Int) {
    }

    fun String.bar() {
    }

}

fun main(args: Array<String>) {
    Object.bar()
    Object.bar(1)
}