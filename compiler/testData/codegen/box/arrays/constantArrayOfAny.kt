// WITH_STDLIB
val x: Any = arrayOf<Any>(arrayOf("OK"))

fun box(): String = ((x as Array<Any>)[0] as Array<String>)[0]
