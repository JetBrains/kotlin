import kotlin.reflect.KProperty

class X
class Y
class Z

operator fun X.getValue(thisRef: Nothing?, property: KProperty<*>): String = ""
operator fun Y.getValue(thisRef: Any?, property: KProperty<*>): String = ""
operator fun Z.getValue(thisRef: Any, property: KProperty<*>): String = ""

val property by <caret>

// EXIST: lazy
// EXIST: X
// EXIST: Y
// ABSENT: Z
