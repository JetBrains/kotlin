class My {
    val x: String

    constructor() {
        val y = bar(this)
        val z = foo()
        x = "$y$z"
    }

    fun foo() = x
}

fun bar(arg: My) = arg.x
