import kotlin.reflect.*
import kotlin.test.assertTrue

interface Interface
annotation class Anno(val x: Int)
object Obj

class C {
    companion object
}

fun box(): String {
    assertTrue(Interface::class.constructors.isEmpty())
    assertTrue(Anno::class.constructors.isEmpty())
    assertTrue(Obj::class.constructors.isEmpty())
    assertTrue(C.Companion::class.constructors.isEmpty())

    return "OK"
}
