// FIR_IDENTICAL
class C<<!REIFIED_TYPE_PARAMETER_NO_INLINE!>reified<!> T>

val <<!REIFIED_TYPE_PARAMETER_NO_INLINE!>reified<!> T> T.v: T
    get() = throw UnsupportedOperationException()

fun <<!REIFIED_TYPE_PARAMETER_NO_INLINE!>reified<!> T> bar() {}