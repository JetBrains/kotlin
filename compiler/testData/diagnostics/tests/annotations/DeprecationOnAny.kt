annotation class Other

@RequiresOptIn(message = "This is a test.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class OptInMarker

class A {
    @Other
    @Deprecated("equals")
    @SinceKotlin("1.2")
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    @SinceKotlin("1.3")
    override fun hashCode(): Int {
        return super.hashCode()
    }

    @Deprecated("toString")
    @OptInMarker
    override fun toString(): String {
        return super.toString()
    }

    @Deprecated("other")
    @OptInMarker
    fun test(): Int = 5
}