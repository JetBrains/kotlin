// !LANGUAGE: +ContextReceivers
// WITH_STDLIB

import kotlin.reflect.KProperty

var operationScore = 0

class Delegate {
    var delegateValue = "fail"

    context(Int)
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        operationScore += this@Int
        return delegateValue
    }

    context(Int)
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        operationScore += this@Int
        delegateValue = value
    }
}

context(Int)
class Result {
    var s: String by Delegate()
}

fun box(): String {
    val result = with(1) { Result() }
    result.s = "OK"
    val returnValue = result.s
    return if (operationScore == 2) returnValue else "fail"
}
