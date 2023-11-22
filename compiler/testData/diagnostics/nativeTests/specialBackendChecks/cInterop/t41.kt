import kotlinx.cinterop.*

fun bar(x: Int) {

    fun foo() = x

    staticCFunction(::foo)
}
