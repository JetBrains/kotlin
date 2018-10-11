// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

interface InlineTrait {

    private inline fun privateInline(s: () -> String): String {
        return s()
    }

    fun testPrivateInline(): String {
        return privateInline { "private" }
    }

    fun testPrivateInline2(): String {
        return privateInline { "private2" }
    }

    companion object {
        inline final fun finalInline(s: () -> String): String {
            return s()
        }
    }
}

class Z : InlineTrait {

}

// FILE: 2.kt

import test.*

fun testClassObject(): String {
    return InlineTrait.finalInline({ "classobject" })
}

fun box(): String {
    if (Z().testPrivateInline() != "private") return "test1: ${Z().testPrivateInline()}"
    if (Z().testPrivateInline2() != "private2") return "test2: ${Z().testPrivateInline2()}"
    if (testClassObject() != "classobject") return "test3: ${testClassObject()}"

    return "OK"
}
