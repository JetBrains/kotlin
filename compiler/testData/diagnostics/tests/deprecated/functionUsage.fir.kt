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
    invocable()
    InvocableHolder.invocable()
}

fun block() {
    Obsolete()
    Obsolete(2)
}

fun expression() = Obsolete()

fun reflection() = ::Obsolete
fun reflection2() = UsefulClass::member

class Initializer {
    val x = Obsolete()
}

@Deprecated("does nothing good")
fun Any.doNothing() = this.toString()  // "this" should not be marked as deprecated despite it referes to deprecated function

class Delegation {
    val x by Obsolete()
    var y by Obsolete()
}
