@RequiresOptIn(message = " ")
annotation class EmptyMarker

@EmptyMarker
fun foo() {}

fun bar() {
    foo()
}