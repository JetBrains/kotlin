// FIR_IDENTICAL
package pack

class DelegatedProperty {
    var baz by action { 1 + 23 }
}

class Action {
    operator fun getValue(thisRef: Any?, property: Any?) = 42
    operator fun setValue(thisRef: Any?, property: Any?, value: Int) {}
}

fun <T> action(action: () -> T): Action = Action()