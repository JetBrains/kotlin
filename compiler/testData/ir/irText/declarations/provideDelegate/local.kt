// FIR_IDENTICAL
// DUMP_LOCAL_DECLARATION_SIGNATURES

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57434

class Delegate(val value: String) {
    operator fun getValue(thisRef: Any?, property: Any?) = value
}

class DelegateProvider(val value: String) {
    operator fun provideDelegate(thisRef: Any?, property: Any?) = Delegate(value)
}

fun foo() {
    val testMember by DelegateProvider("OK")
}

