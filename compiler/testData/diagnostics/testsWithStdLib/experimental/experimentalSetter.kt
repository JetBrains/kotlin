// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn

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
    <!OPT_IN_USAGE_ERROR!>x<!> = 10
    <!OPT_IN_USAGE_ERROR!>y<!> = 5
    <!OPT_IN_USAGE_ERROR!>z<!> = 15
    return x + <!OPT_IN_USAGE_ERROR!>y<!> + z
}
