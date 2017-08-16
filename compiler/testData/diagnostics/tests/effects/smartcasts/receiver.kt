// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +ContractEffects

import kotlin.internal.*

@Returns(ConstantValue.FALSE)
fun @receiver:Equals(ConstantValue.NOT_NULL) Any?.isNull() = this == null

fun smartcastOnReceiver(x: Int?) {
    if (x.isNull()) {
        x<!UNSAFE_CALL!>.<!>inc()
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.dec()
    }
}

class UnstableReceiver {
    var x: Int? = 42

    fun smartcastOnUnstableReceiver() {
        if (x.isNull()) {
            x<!UNSAFE_CALL!>.<!>inc()
        } else {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.dec()
        }
    }
}

