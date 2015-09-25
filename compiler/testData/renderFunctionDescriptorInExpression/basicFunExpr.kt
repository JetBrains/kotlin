annotation class Ann(val x: String)

fun foo(block: () -> Unit) = block.javaClass

fun box() {
    foo( @Ann("OK1") fun() {})
}

//@Ann(x = "OK1") local final fun <no name provided>(): kotlin.Unit defined in box