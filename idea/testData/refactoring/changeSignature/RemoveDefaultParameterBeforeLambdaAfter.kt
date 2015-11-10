fun foo(a: String, c: Boolean = false, block: (String) -> Unit) {
    block(a)
}

fun test() {
    foo("Hello", c = true) {
        println(it)
    }

    foo("Hello", true) {
        println(it)
    }

    foo("Hello", c = true) {
        println(it)
    }

    foo("Hello") {
        println(it)
    }

    foo("Hello") {
        println(it)
    }

    foo("Hello") {
        println("Don't let $it go!")
    }
}