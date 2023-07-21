// !SKIP_TXT
// DUMP_CFG

import kotlin.reflect.KClass

fun exc(flag: Boolean) {
    if (flag) throw RuntimeException()
}

fun Any.notNull() = toString()

fun test(flag: Boolean) {
    var x: Any?
    x = ""
    try {
        x = null
        exc(flag)
        x = 1
        exc(!flag)
        x = ""
    } catch (e: Throwable) {
        // all bad - could come here from either call
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun testGetClassThrows() {
    var x: KClass<String>? = String::class
    x as KClass<String>
    try {
        x = null
        x = String::class
    } catch (e: Throwable) {
        // bad - get class call can throw
        x<!UNSAFE_CALL!>.<!>notNull()
    }
}

fun testMemberReferenceThrows() {
    var x: Any? = ""
    x as Any
    try {
        x = null
        x = String::length
    } catch (ex: Throwable) {
        // bad - get callable reference throw
        x<!UNSAFE_CALL!>.<!>notNull()
    }
}

fun testExceptionBeforeLambda() {
    var x: String? = ""
    x as String
    try {
        x = null
        run { x = "" }
    } catch (ex: Throwable) {
        // bad - `run` could throw before running lambda
        x<!UNSAFE_CALL!>.<!>notNull()
    }
}

fun testExceptionWithinLocalFunction() {
    var x: Any = ""
    x as String
    try {
        fun local() {
            x = 1
        }
    } catch (e: Exception) {
        // bad - `local` could be run and reset smartcasting
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
