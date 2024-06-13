// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE

open class View

fun test() {
    val target = foo<View>() ?: foo() ?: run {}
}

fun <T : View> foo(): T? {
    return null
}
