fun <caret>foo(a: String, b: Boolean = false, c: Boolean = false, block: (String) -> Unit) {
    block(a)
}

fun test() {
    foo("Hello", b = false, c = true) {
        println(it)
    }

    foo("Hello", false, true) {
        println(it)
    }

    foo("Hello", c = true) {
        println(it)
    }

    foo("Hello", b = false) {
        println(it)
    }

    foo("Hello", false) {
        println(it)
    }

    foo("Hello") {
        println("Don't let $it go!")
    }
}