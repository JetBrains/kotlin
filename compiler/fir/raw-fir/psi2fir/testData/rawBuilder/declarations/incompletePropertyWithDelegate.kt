// IGNORE_TREE_ACCESS: KT-64898
import kotlin.reflect.KProperty

fun String.getValue(x: Any?, y: KProperty<*>) = ""
val x: String by
