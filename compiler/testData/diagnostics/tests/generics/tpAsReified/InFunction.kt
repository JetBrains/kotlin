// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline fun <reified T> f(): T = throw UnsupportedOperationException()

fun <T> id(p: T): T = p

fun <A> main() {
    <!NI;REIFIED_TYPE_FORBIDDEN_SUBSTITUTION, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>f<!>()

    <!NI;UNREACHABLE_CODE!>val <!OI;UNUSED_VARIABLE!>a<!>: A = <!TYPE_PARAMETER_AS_REIFIED!>f<!>()<!>
    <!NI;UNREACHABLE_CODE!>f<<!TYPE_PARAMETER_AS_REIFIED!>A<!>>()<!>

    <!NI;UNREACHABLE_CODE!>val <!OI;UNUSED_VARIABLE!>b<!>: Int = f()<!>
    <!NI;UNREACHABLE_CODE!>f<Int>()<!>

    <!NI;UNREACHABLE_CODE!>val <!OI;UNUSED_VARIABLE!>с<!>: A = id(<!TYPE_PARAMETER_AS_REIFIED!>f<!>())<!>
}