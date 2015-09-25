annotation class Ann(val x: String)

fun foo(block: () -> Unit) = block.javaClass

fun box() {
    foo( @Ann("OK1") label@{ })

    foo() @Ann("OK2") label@{ }
}

//@Ann(x = "OK1") local final fun <anonymous>(): kotlin.Unit defined in box
//@Ann(x = "OK2") local final fun <anonymous>(): kotlin.Unit defined in box
