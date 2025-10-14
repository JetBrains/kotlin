// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.coroutines.SuspendFunction0
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
    assertTrue(Function0::class.constructors.isEmpty())
    assertTrue(SuspendFunction0::class.constructors.isEmpty())

    return "OK"
}
