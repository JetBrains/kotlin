// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +ContractEffects

import kotlin.internal.*

@Returns(ConstantValue.TRUE)
fun isString(@IsInstance(String::class) x: Any?) = x is String

class C<T>(<!UNUSED_PARAMETER!>t<!> :T)

fun test1(a: Any) {
    if (isString(a)) {
        val <!UNUSED_VARIABLE!>c<!>: C<String> = C(<!DEBUG_INFO_SMARTCAST!>a<!>)
    }
}


fun <T> f(t :T): C<T> = C(t)

fun test2(a: Any) {
    if (isString(a)) {
        val <!UNUSED_VARIABLE!>c1<!>: C<String> = f(<!DEBUG_INFO_SMARTCAST!>a<!>)
    }
}
