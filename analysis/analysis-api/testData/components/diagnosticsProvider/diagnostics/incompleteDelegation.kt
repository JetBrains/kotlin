import kotlin.reflect.KProperty

fun String.getValue(x: Any?, y: KProperty<*>) = ""
val x: String by <caret>