// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertTrue

interface Interface
object Obj

class C {
    companion object
}

fun box(): String {
    assertTrue(Interface::class.constructors.isEmpty())
    assertTrue(Obj::class.constructors.isEmpty())
    assertTrue(C.Companion::class.constructors.isEmpty())
    assertTrue(object {}::class.constructors.isEmpty())

    return "OK"
}
