suspend fun testB(x: Int) {}

suspend fun testA() {
    testB(123)
    testB(456)
}

class A {
    suspend fun testC() {
        testA()
    }
}

fun getSimpleName0(x: suspend () -> Unit) = x::class.simpleName
fun getSimpleName1(x: suspend (x: Int) -> Unit) = x::class.simpleName
inline fun <reified T> getSimpleNameReified(x: T) = T::class.simpleName

fun box(): String {
    assertEquals("Function1", getSimpleName0(::testA))
    assertEquals("Function2", getSimpleName1(::testB))
    assertEquals("Function1", getSimpleName0(A()::testC))

    assertEquals("KSuspendFunction0", getSimpleNameReified(::testA))
    assertEquals("KSuspendFunction1", getSimpleNameReified(::testB))
    assertEquals("KSuspendFunction0", getSimpleNameReified(A()::testC))

    assertEquals("Function1", (::testA)::class.simpleName)
    assertEquals("Function2", (::testB)::class.simpleName)
    assertEquals("Function1", (A()::testC)::class.simpleName)
    return "OK"
}
