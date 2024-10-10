// FIR_IDENTICAL
package pack

val myProperty: Int by action {
    0 + 1
}

class Action {
    operator fun getValue(thisRef: Any?, property: Any?) = 42
    operator fun setValue(thisRef: Any?, property: Any?, value: Int) {}
}

fun <T> action(action: () -> T): Action = Action()