// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.coroutines.SuspendFunction2
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.test.assertEquals

class DefaultVisibilityClass
public class PublicClass {
    protected class ProtectedClass
    fun getProtectedClass(): KClass<*> = ProtectedClass::class
}
internal class InternalClass
private class PrivateClass

fun box(): String {
    assertEquals(KVisibility.PUBLIC, DefaultVisibilityClass::class.visibility)
    assertEquals(KVisibility.PUBLIC, PublicClass::class.visibility)
    assertEquals(KVisibility.PROTECTED, PublicClass().getProtectedClass().visibility)
    assertEquals(KVisibility.INTERNAL, InternalClass::class.visibility)
    assertEquals(KVisibility.PRIVATE, PrivateClass::class.visibility)

    class Local
    assertEquals(null, Local::class.visibility)

    val anonymous = object {}
    assertEquals(null, anonymous::class.visibility)

    assertEquals(KVisibility.PUBLIC, Function1::class.visibility)
    assertEquals(KVisibility.PUBLIC, SuspendFunction2::class.visibility)

    return "OK"
}
