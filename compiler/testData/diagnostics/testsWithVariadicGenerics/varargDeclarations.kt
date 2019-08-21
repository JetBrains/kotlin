// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Box<T>

fun <vararg Ts> simple(vararg args: *Ts) {}
fun <vararg Ts> wrapped(vararg args: *Box<Ts>) {}
fun <vararg Ts> middleWrapped(vararg args: Box<*Box<Ts>>) {}
fun <vararg Ts> doubleWrapped(vararg args: *Box<Box<Ts>>) {}