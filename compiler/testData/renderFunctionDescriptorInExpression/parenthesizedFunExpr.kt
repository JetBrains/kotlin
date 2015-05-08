annotation class Ann(val x: String)

fun foo(block: () -> Unit) = block.javaClass

fun box() {
    foo( @Ann("OK1") (fun() {}))
}

//local final fun <no name provided>(): kotlin.Unit defined in box