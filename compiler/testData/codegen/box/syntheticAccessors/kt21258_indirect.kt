class Foo {
    private val fld: String = "O"
        get() = { field }() + "K"

    val indirectFldGetter: () -> String = { fld }
}

fun box() = Foo().indirectFldGetter()
