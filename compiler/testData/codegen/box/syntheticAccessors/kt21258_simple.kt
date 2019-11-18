// IGNORE_BACKEND_FIR: JVM_IR
class Foo {
    private val fld: String = "O"
        get() = { field }() + "K"

    val indirectFldGetter: () -> String = { fld }

    fun simpleFldGetter(): String {
        return fld
    }
}

fun box() = Foo().simpleFldGetter()
