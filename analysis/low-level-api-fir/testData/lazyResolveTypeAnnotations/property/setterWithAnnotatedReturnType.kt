// IGNORE_FIR
// KT-77554: the implicit type phase transforms the explicit setter return type with lazy annotation arguments
@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

class A {
    var pr<caret>operty = 1
        get() = field
        set(value): @Anno("setter return type $prop") Unit {
            field = value
        }
}
