// FILE: 1.kt

package test

inline fun inline(s: () -> String): String {
    return s()
}

class InlineAll {

    inline fun inline(s: () -> String): String {
        return s()
    }

    companion object {
        inline fun inline(s: () -> String): String {
            return s()
        }
    }
}

// FILE: 2.kt

import test.*

fun testClassObjectCall(): String {
    return InlineAll.inline({"classobject"})
}

fun testInstanceCall(): String {
    val inlineX = InlineAll()
    return inlineX.inline({"instance"})
}

fun testPackageCall(): String {
    return inline({"package"})
}

fun box(): String {
    if (testClassObjectCall() != "classobject") return "test1: ${testClassObjectCall()}"
    if (testInstanceCall() != "instance") return "test2: ${testInstanceCall()}"
    if (testPackageCall() != "package") return "test3: ${testPackageCall()}"
    return "OK"
}
