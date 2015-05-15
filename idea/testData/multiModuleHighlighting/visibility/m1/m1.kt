package m1

public class PublicClassInM1
class InternalClassInM1
private class PrivateClassInM1

public fun publicFunInM1() {
}
fun internalFunInM1() {
}
private fun privateFunInM1() {
}

fun testVisibility() {
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: ClassInM2">ClassInM2</error>()
}

public open class A internal constructor() {
    private fun pri() {
    }
    fun int() {
    }
    protected fun pro() {
    }
    public fun pub() {
    }
}