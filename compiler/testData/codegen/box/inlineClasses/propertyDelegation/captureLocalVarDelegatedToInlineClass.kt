// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

var setterInvoked = 0

var backing = 42

inline class DelegateStr(val ignored: String) {

    operator fun getValue(thisRef: Any?, prop: Any?) =
        backing

    operator fun setValue(thisRef: Any?, prop: Any?, newValue: Int) {
        setterInvoked++
        backing = newValue
    }
}

inline class DelegateInt(val ignored: Int) {

    operator fun getValue(thisRef: Any?, prop: Any?) =
        backing

    operator fun setValue(thisRef: Any?, prop: Any?, newValue: Int) {
        setterInvoked++
        backing = newValue
    }
}

inline class DelegateLong(val ignored: Long) {

    operator fun getValue(thisRef: Any?, prop: Any?) =
        backing

    operator fun setValue(thisRef: Any?, prop: Any?, newValue: Int) {
        setterInvoked++
        backing = newValue
    }
}

fun box(): String {
    setterInvoked = 0
    testDelegateStr()
    if (setterInvoked != 1) throw AssertionError()

    setterInvoked = 0
    testDelegateInt()
    if (setterInvoked != 1) throw AssertionError()

    setterInvoked = 0
    testDelegateLong()
    if (setterInvoked != 1) throw AssertionError()

    return "OK"
}

private fun testDelegateStr() {
    var localD by DelegateStr("don't care")

    return {
        if (localD != 42) AssertionError()

        localD = 1234
        if (localD != 1234) throw AssertionError()
        if (backing != 1234) throw AssertionError()
    }()
}

private fun testDelegateInt() {
    var localD by DelegateInt(999)

    return {
        if (localD != 42) AssertionError()

        localD = 1234
        if (localD != 1234) throw AssertionError()
        if (backing != 1234) throw AssertionError()
    }()
}

private fun testDelegateLong() {
    var localD by DelegateLong(999L)

    return {
        if (localD != 42) AssertionError()

        localD = 1234
        if (localD != 1234) throw AssertionError()
        if (backing != 1234) throw AssertionError()
    }()
}