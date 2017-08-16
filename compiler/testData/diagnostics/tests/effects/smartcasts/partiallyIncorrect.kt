// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +ContractEffects

import kotlin.internal.*

@Returns(ConstantValue.TRUE)
fun isString(@IsInstance(String::class) x: Any?) = x is String

fun incorrectPartDoesntMatter(x: Any?) {
    if (isString(x) && <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}