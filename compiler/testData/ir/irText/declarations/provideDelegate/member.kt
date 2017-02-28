class Delegate(val value: String) {
    operator fun getValue(thisRef: Any?, property: Any?) = value
}

class DelegateProvider(val value: String) {
    operator fun provideDelegate(thisRef: Any?, property: Any?) = Delegate(value)
}

class Host {
    val testMember by DelegateProvider("OK")
}

