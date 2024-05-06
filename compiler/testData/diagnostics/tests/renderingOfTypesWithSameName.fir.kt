// RENDER_DIAGNOSTICS_FULL_TEXT
class MutableVector<T>(
    var content: Array<T>,
) {
    inline fun <reified T: Any> foo(block: (T) -> Unit) {
        block(<!ARGUMENT_TYPE_MISMATCH!>content[0]<!>)
    }
}

interface Consumer<T> {
    fun accept(t: T): T
}

fun f(c: Consumer<in String>) {
    c.accept(<!ARGUMENT_TYPE_MISMATCH!>c.accept(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!>)
}