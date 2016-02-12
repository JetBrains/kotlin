// See KT-11007: Wrong smart cast to not-null type after safe calls in if / when expression

val String.copy: String
    get() = this

fun foo() {
    val s: String? = null
    val ss = if (true) {
        s?.length
    } else {
        s?.length
    }
    ss<!UNSAFE_CALL!>.<!>hashCode() // Smart-cast to Int, should be unsafe call
    val sss = if (true) {
        s?.copy
    }
    else {
        s?.copy
    }
    sss<!UNSAFE_CALL!>.<!>length
}

class My {
    val String.copy2: String
        get() = this

    fun foo() {
        val s: String? = null
        val ss = if (true) {
            s?.length
        } else {
            s?.length
        }
        ss<!UNSAFE_CALL!>.<!>hashCode()
        val sss = if (true) {
            s?.copy2
        }
        else {
            s?.copy2
        }
        sss<!UNSAFE_CALL!>.<!>length
    }
}
