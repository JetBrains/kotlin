annotation class Other

@RequiresOptIn(message = "This is a test.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class OptInMarker

class A {
    @Other
    <!POTENTIALLY_NON_REPORTED_ANNOTATION!>@Deprecated("equals")<!>
    <!POTENTIALLY_NON_REPORTED_ANNOTATION!>@SinceKotlin("1.2")<!>
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    <!POTENTIALLY_NON_REPORTED_ANNOTATION!>@SinceKotlin("1.3")<!>
    override fun hashCode(): Int {
        return super.hashCode()
    }

    <!POTENTIALLY_NON_REPORTED_ANNOTATION!>@Deprecated("toString")<!>
    <!POTENTIALLY_NON_REPORTED_ANNOTATION!>@OptInMarker<!>
    override fun toString(): String {
        return super.toString()
    }

    @Deprecated("other")
    @OptInMarker
    fun test(): Int = 5
}