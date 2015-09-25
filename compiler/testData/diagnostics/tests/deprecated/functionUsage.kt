// !DIAGNOSTICS: -UNUSED_EXPRESSION

class UsefulClass(val param: Int = 2) {
    fun get(instance: Any, property: PropertyMetadata) : Int = 1
    fun set(instance: Any, property: PropertyMetadata, value: Int) {}

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
    <!DEPRECATED_SYMBOL_WITH_MESSAGE!>invocable<!>()
    InvocableHolder.<!DEPRECATED_SYMBOL_WITH_MESSAGE!>invocable<!>()
}

fun block() {
    <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()
    <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>(2)
}

fun expression() = <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()

fun reflection() = ::<!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>
fun reflection2() = UsefulClass::<!DEPRECATED_SYMBOL_WITH_MESSAGE!>member<!>

class Initializer {
    val x = <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()
}

@Deprecated("does nothing good")
fun Any.doNothing() = this.toString()  // "this" should not be marked as deprecated despite it referes to deprecated function

class Delegation {
    val x by <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()
    var y by <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()
}