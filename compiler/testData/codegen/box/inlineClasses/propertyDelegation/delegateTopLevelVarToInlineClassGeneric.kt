// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

var setterInvoked = 0
var backing = 42

OPTIONAL_JVM_INLINE_ANNOTATION
value class Delegate<T: Int>(val ignored: T) {

    operator fun getValue(thisRef: Any?, prop: Any?) =
        backing

    operator fun setValue(thisRef: Any?, prop: Any?, newValue: Int) {
        setterInvoked++
        backing = newValue
    }
}

var topLevelD by Delegate(0)

fun box(): String {
    if (topLevelD != 42) AssertionError()

    topLevelD = 1234
    if (topLevelD != 1234) throw AssertionError()
    if (backing != 1234) throw AssertionError()

    if (setterInvoked != 1) throw AssertionError()

    return "OK"
}