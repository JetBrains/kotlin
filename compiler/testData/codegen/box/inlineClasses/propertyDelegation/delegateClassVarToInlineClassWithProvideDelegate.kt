// WITH_STDLIB

class Foo {
    var a: Int = 42
    var d by DelegateFactory(0)
}

var provideDelegateInvoked = 0
var setterInvoked = 0

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class DelegateFactory(val default: Int) {
    operator fun provideDelegate(thisRef: Any?, prop: Any?): Delegate {
        provideDelegateInvoked++
        return Delegate(default)
    }
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Delegate(val default: Int) {

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