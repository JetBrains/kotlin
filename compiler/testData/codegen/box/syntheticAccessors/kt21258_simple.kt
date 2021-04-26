fun <T> eval(fn: () -> T) = fn()

class Foo {
    private val fld: String = "O"
        get() = eval { field } + "K"

    val indirectFldGetter: () -> String = { fld }

    fun simpleFldGetter(): String {
        return fld
    }
}

fun box() = Foo().simpleFldGetter()
