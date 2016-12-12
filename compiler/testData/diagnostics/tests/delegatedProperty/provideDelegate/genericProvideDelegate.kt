// !DIAGNOSTICS: -UNUSED_PARAMETER

class Cell<out V>(val value: V)

class GenericDelegate<V>(val value: V)

operator fun <T> T.provideDelegate(a: Any?, p: Any?) = GenericDelegate(this)

operator fun <W> GenericDelegate<W>.getValue(a: Any?, p: Any?) = Cell(value)

val test1: Cell<String> by "OK"
val test2: Cell<Any> by "OK"
val test3 by "OK"