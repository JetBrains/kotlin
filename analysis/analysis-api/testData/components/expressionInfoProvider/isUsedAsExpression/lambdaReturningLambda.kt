fun <T> block(fn: () -> T): T = fn()

fun foo(x: Int) {
    val x: (Int) -> String = block {
        if (x == 0) {
             <expr>{ x: Int ->
                x.toString()
            }</expr>
        }
        else {
            { _: Int ->
                "Hello"
            }
        }
    }

    x(4)
}