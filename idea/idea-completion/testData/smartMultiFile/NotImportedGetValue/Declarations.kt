package dependency

import kotlin.reflect.KProperty
import main.C

class X1 {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""
}
class X2 {
    operator fun getValue(thisRef: String, property: KProperty<*>): String = ""
}
class X3 {
    operator fun getValue(thisRef: Any, property: KProperty<*>): String = ""
}

class Y1
class Y2
class Y3

operator fun Y1.getValue(thisRef: C, property: KProperty<*>): String = ""
operator fun Y2.getValue(thisRef: String, property: KProperty<*>): String = ""
operator fun Y3.getValue(thisRef: Any, property: KProperty<*>): String = ""
