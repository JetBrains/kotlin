package codegen.inline.twiceInlinedObject

import kotlin.test.*

inline fun exec(f: () -> Unit) = f()

inline fun test2() {
    val obj = object {
        fun sayOk() = println("Ok")
    }
    obj.sayOk()
}

inline fun noExec(f: () -> Unit) { }

@Test fun runTest() {
    exec {
        test2()
    }
    noExec {
        test2()
    }
}