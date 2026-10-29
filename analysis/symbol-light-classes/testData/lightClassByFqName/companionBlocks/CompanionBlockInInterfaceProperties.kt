// one.I
// LANGUAGE: +CompanionBlocks
// WITH_STDLIB
package one

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = "delegated"
}

interface I {
    companion {
        val pub: String = "pub"
        internal val intern: String = "intern"
        private val priv: String = "priv"
        val del: String by Delegate()
        var mut: Int = 0
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: I.class[priv]
