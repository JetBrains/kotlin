// !LANGUAGE: +InlineClasses
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

class Foo {
    companion object {
        var a: Int = 42
        @JvmStatic var d by Delegate(0)
    }
}

var setterInvoked = 0

inline class Delegate(val ignored: Int) {

    operator fun getValue(thisRef: Any?, prop: Any?) = Foo.a

    operator fun setValue(thisRef: Any?, prop: Any?, newValue: Int) {
        setterInvoked++
        Foo.a = newValue
    }
}


fun box(): String {
    if (Foo.d != 42) throw AssertionError()

    Foo.d = 1234
    if (Foo.d != 1234) throw AssertionError()
    if (Foo.a != 1234) throw AssertionError()

    if (setterInvoked != 1) throw AssertionError()

    return "OK"
}