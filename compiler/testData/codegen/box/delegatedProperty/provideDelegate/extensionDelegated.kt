import kotlin.reflect.KProperty

var log = ""

class UserDataProperty<in R>(val key: String) {
    operator fun getValue(thisRef: R, desc: KProperty<*>) = thisRef.toString() + key

    operator fun setValue(thisRef: R, desc: KProperty<*>, value: String?) { log += "set"}
}


var String.calc: String by UserDataProperty("K")

fun box(): String {
    return "O".calc
}
