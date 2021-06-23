// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION

import kotlin.reflect.KProperty

class UsefulClass(val param: Int = 2) {
    operator fun getValue(instance: Any, property: KProperty<*>) : Int = 1
    operator fun setValue(instance: Any, property: KProperty<*>, value: Int) {}

    @Deprecated("message")
    fun member() {}
}

@Deprecated("message")
fun Obsolete(param: Int = 1): UsefulClass = UsefulClass(param)

class Invocable {
    @Deprecated("message")
    operator fun invoke() {}
}

object InvocableHolder {
    val invocable = Invocable()
}

fun invoker() {
    val invocable = Invocable()
    <!DEPRECATION!>invocable<!>()
    InvocableHolder.<!DEPRECATION!>invocable<!>()
}

fun block() {
    <!DEPRECATION!>Obsolete<!>()
    <!DEPRECATION!>Obsolete<!>(2)
}

fun expression() = <!DEPRECATION!>Obsolete<!>()

fun reflection() = ::<!DEPRECATION!>Obsolete<!>
fun reflection2() = UsefulClass::<!DEPRECATION!>member<!>

class Initializer {
    val x = <!DEPRECATION!>Obsolete<!>()
}

@Deprecated("does nothing good")
fun Any.doNothing() = this.toString()  // "this" should not be marked as deprecated despite it referes to deprecated function

class Delegation {
    val x by <!DEPRECATION!>Obsolete<!>()
    var y by <!DEPRECATION!>Obsolete<!>()
}
