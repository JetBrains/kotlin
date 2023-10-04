// ISSUE: KT-62114

open class A {
    class B : A() {
        val a = "FAIL"
    }

    class C : A() {
        val a = "FATAL"
    }

    fun foo(): String {
        if (this is B) return <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>a<!>
        else if (this is C) return <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>a<!>
        return "OK"
    }
}

fun A?.bar() {
    if (this != null) <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>foo<!>()
}

fun A.gav() = if (this is A.B) <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>a<!> else ""

class C {
    fun A?.complex(): String {
        if (this is A.B) return <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>a<!>
        else if (this != null) return <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>foo<!>()
        else return ""
    }
}

sealed class Received<out T> {
    sealed class Error<out T> : Received<T>() {
        data class SomeError<out T>(val details: T?) : Error<T>()
    }
}

val Received<String>.thisRaisesUnresolvedReference: Boolean
    get() = if (this is Received.Error<*>) {
        when (<!DEBUG_INFO_SMARTCAST!>this<!>) {
            is Received.Error.SomeError -> <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>details<!>?.length == 0
        }
    } else {
        false
    }

val Received<String>.thisIsFine: Boolean
    get() = if (this is Received.Error<*>) {
        if (this is Received.Error.SomeError) { <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>details<!>?.length == 0 }
        else false
    } else {
        false
    }
