@RequiresOptIn
@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION)
annotation class Marker

class Wrapper<T>

@Marker
typealias TA<T> = Wrapper<T>

open class Base<T> {
    @Marker
    open fun foo(): T? = null

    open fun bar(): <!OPT_IN_USAGE_ERROR!>TA<T><!>? = null
}

class Derived : Base<String>()

fun test(d: Derived) {
    d.<!OPT_IN_USAGE_ERROR!>foo<!>()
    d.<!OPT_IN_USAGE_ERROR!>bar<!>()
}
