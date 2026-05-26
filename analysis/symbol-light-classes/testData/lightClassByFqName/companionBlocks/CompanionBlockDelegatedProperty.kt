// one.C
// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
package one

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = "delegated"
}

class C {
    companion {
        val delegated: String by Delegate()
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: C.class[delegated$delegate;getDelegated]
