// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline fun <reified T> f(): T = throw UnsupportedOperationException()

fun <T> id(p: T): T = p

fun <A> main() {
    <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>f<!>()

    val <!UNUSED_VARIABLE!>a<!>: A = <!TYPE_PARAMETER_AS_REIFIED!>f<!>()
    f<<!TYPE_PARAMETER_AS_REIFIED!>A<!>>()

    val <!UNUSED_VARIABLE!>b<!>: Int = f()
    f<Int>()

    val <!UNUSED_VARIABLE!>с<!>: A = id(<!NI;TYPE_PARAMETER_AS_REIFIED, TYPE_PARAMETER_AS_REIFIED!>f<!>())
}