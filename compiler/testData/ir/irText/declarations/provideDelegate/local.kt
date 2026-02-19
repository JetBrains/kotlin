// FIR_IDENTICAL

class Delegate(val value: String) {
    operator fun getValue(thisRef: Any?, property: Any?) = value
}

class DelegateProvider(val value: String) {
    operator fun provideDelegate(thisRef: Any?, property: Any?) = Delegate(value)
}

fun foo() {
    val testMember by DelegateProvider("OK")
}

