// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
// !LANGUAGE: +InlineClasses

var setterInvoked = 0
var backing = 42

inline class Delegate(val ignored: Int) {

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