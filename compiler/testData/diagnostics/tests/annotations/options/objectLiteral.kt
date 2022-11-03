// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
@Target(AnnotationTarget.CLASS)
annotation class Ann

open class My

fun foo(): My {
    return (@Ann object: My() {})
}