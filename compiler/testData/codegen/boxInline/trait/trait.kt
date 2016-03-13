// FILE: 1.kt

package test

interface InlineTrait {

    fun finalInline(s: () -> String): String {
        return s()
    }

    companion object {
        inline final fun finalInline(s: () -> String): String {
            return s()
        }
    }
}

class Z: InlineTrait {

}

// FILE: 2.kt

import test.*

fun testFinalInline(): String {
    return Z().finalInline({"final"})
}

fun testFinalInline2(instance: InlineTrait): String {
    return instance.finalInline({"final2"})
}

fun testClassObject(): String {
    return InlineTrait.finalInline({"classobject"})
}

fun box(): String {
    if (testFinalInline() != "final") return "test1: ${testFinalInline()}"
    if (testFinalInline2(Z()) != "final2") return "test2: ${testFinalInline2(Z())}"
    if (testClassObject() != "classobject") return "test3: ${testClassObject()}"

    return "OK"
}
