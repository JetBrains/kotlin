import kotlinx.cinterop.*

fun <T: CVariable> bar() {

    fun foo(x: CValue<T>) = x

    staticCFunction(::foo)
}
