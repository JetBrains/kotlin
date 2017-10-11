package runtime.basic.entry0

fun fail() {
    println("Test failed, this is a wrong main() function.")
}

fun main() {
    fail()
}

fun <T> main(args: Array<String>) {
    fail()
}

fun main(args: Array<Int>) {
    fail()
}

fun main(args: Array<String>, second_arg: Int) {
    fail()
}

class Foo {
    fun main(args: Array<String>) {
        fail()
    }
}

fun main(args: Array<String>) {
    println("Hello.")
}

