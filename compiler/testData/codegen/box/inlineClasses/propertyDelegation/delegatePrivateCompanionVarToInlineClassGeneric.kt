// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

class Foo {
    companion object {
        var a: Int = 42
        private var d by Delegate(0)

        fun d() = d
        fun d(newValue: Int) { d = newValue }
    }
}

var setterInvoked = 0

OPTIONAL_JVM_INLINE_ANNOTATION
value class Delegate<T: Int>(val ignored: T) {

    operator fun getValue(thisRef: Any?, prop: Any?) = Foo.a

    operator fun setValue(thisRef: Any?, prop: Any?, newValue: Int) {
        setterInvoked++
        Foo.a = newValue
    }
}


fun box(): String {
    if (Foo.d() != 42) throw AssertionError()

    Foo.d(1234)
    if (Foo.d() != 1234) throw AssertionError()
    if (Foo.a != 1234) throw AssertionError()

    if (setterInvoked != 1) throw AssertionError()

    return "OK"
}