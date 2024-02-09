// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_EXPRESSION

inline fun <reified T> foo() {
    <!CALLABLE_REFERENCE_LHS_NOT_A_CLASS!>T::toString<!>
}

inline fun <reified T> f(): T = throw UnsupportedOperationException()

fun <T> id(p: T): T = p

fun <A> main() {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>f<!>()

    val a: A = <!TYPE_PARAMETER_AS_REIFIED!>f<!>()
    f<<!TYPE_PARAMETER_AS_REIFIED!>A<!>>()

    val b: Int = f()
    f<Int>()

    val с: A = id(<!TYPE_PARAMETER_AS_REIFIED!>f<!>())
}
