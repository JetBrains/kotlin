// FIR_IDENTICAL
// WITH_STDLIB

import kotlin.reflect.KProperty

class DelegateProvider {
    operator fun provideDelegate(
        thisRef: Nothing?,
        prop: KProperty<*>
    ): RDelegate = TODO()
}

class RDelegate {
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): String = ""
}

fun builder(name: String? = null, block: () -> Unit): DelegateProvider = TODO()
val x by builder {}

fun main() {
    x.length
}
