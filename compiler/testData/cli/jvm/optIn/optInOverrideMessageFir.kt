@RequiresOptIn(message = "This API is experimental and can change at any time, please use with care")
annotation class Marker

@RequiresOptIn
annotation class EmptyMarker

interface Base {
    @Marker
    fun foo()

    @EmptyMarker
    fun bar()
}

class Derived : Base {
    override fun foo() {}

    override fun bar() {}
}
