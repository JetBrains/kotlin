// SKIP_ERRORS_AFTER

class B(val name: String?) {
    fun checkName(): Boolean {
        return true
    }
}

fun unsafeFoo() {
    val b = B("")
    b.name<caret>!!.length
}