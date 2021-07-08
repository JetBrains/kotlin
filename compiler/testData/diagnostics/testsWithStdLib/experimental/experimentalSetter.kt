// FIR_IDENTICAL
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class E

@set:E
var x: Int = 42

@E
var y: Int = 24

var z: Int = 44
    @E set(arg) {
        field = arg
    }

fun user(): Int {
    <!EXPERIMENTAL_API_USAGE_ERROR!>x<!> = 10
    <!EXPERIMENTAL_API_USAGE_ERROR!>y<!> = 5
    <!EXPERIMENTAL_API_USAGE_ERROR!>x<!> = 15
    return x + <!EXPERIMENTAL_API_USAGE_ERROR!>y<!> + z
}
