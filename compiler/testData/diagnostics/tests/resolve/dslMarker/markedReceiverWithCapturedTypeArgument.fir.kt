@DslMarker
annotation class AnnMarker

@AnnMarker
class Inv<T> {
    fun bar() {}
}

fun Inv<*>.foo() {
    bar()
}