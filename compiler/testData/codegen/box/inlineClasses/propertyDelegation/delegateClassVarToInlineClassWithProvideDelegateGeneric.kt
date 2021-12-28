// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

class Foo {
    var a: Int = 42
    var d by DelegateFactory(0)
}

var provideDelegateInvoked = 0
var setterInvoked = 0

OPTIONAL_JVM_INLINE_ANNOTATION
value class DelegateFactory<T: Int>(val default: T) {
    operator fun provideDelegate(thisRef: Any?, prop: Any?): Delegate<T> {
        provideDelegateInvoked++
        return Delegate(default)
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Delegate<T: Int>(val default: T) {

    operator fun getValue(thisRef: Any?, prop: Any?) =
        (thisRef as? Foo)?.a ?: default

    operator fun setValue(thisRef: Any?, prop: Any?, newValue: Int) {
        setterInvoked++
        if (thisRef is Foo) {
            thisRef.a = newValue
        }
    }
}


fun box(): String {
    val x = Foo()
    if (x.d != 42) throw AssertionError()

    x.d = 1234
    if (x.d != 1234) throw AssertionError()
    if (x.a != 1234) throw AssertionError()

    if (setterInvoked != 1) throw AssertionError()
    if (provideDelegateInvoked != 1) throw AssertionError()

    return "OK"
}