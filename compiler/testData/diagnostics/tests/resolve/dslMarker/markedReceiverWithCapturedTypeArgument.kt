// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
@DslMarker
annotation class AnnMarker

@AnnMarker
class Inv<T> {
    fun bar() {}
}

fun Inv<*>.foo() {
    bar()
}