// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN

sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

infix fun Any.sealed(a: Any?) {}

fun WhenWithSealed(x: Any) {
    val <!NAME_SHADOWING!>x<!> = 1 <!UNSUPPORTED_SEALED_WHEN!>sealed<!> when (x) {
        is BooleanHolder <!UNSUPPORTED_FEATURE!>if x.value<!> -> 1
        is BooleanHolder <!UNSUPPORTED_FEATURE!>if !x.value<!> -> 2
        else -> 3
    }
}
