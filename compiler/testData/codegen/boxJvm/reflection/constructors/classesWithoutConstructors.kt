// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.coroutines.SuspendFunction0
import kotlin.test.assertEquals

interface Interface
object Obj

class C {
    companion object
}

fun box(): String {
    assertEquals(emptyList(), Interface::class.constructors)
    assertEquals(emptyList(), Obj::class.constructors)
    assertEquals(emptyList(), C.Companion::class.constructors)
    assertEquals(emptyList(), object {}::class.constructors)
    assertEquals(emptyList(), Function0::class.constructors)
    assertEquals(emptyList(), SuspendFunction0::class.constructors)

    return "OK"
}
