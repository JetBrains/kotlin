// TARGET_BACKEND: JVM

// WITH_RUNTIME

package zzz
import kotlin.reflect.KMutableProperty1
import kotlin.test.assertEquals

class A(val s1: String, val s2: String) {
    @JvmField public var publicField = s1;
    @JvmField internal var internalField = s2;

    fun testAccessors() {
        val kMutableProperty: KMutableProperty1<A, String> = A::publicField
        checkAccessor(kMutableProperty, s1, "3", this)
        checkAccessor(A::internalField, s2, "4", this)
    }
}

fun box(): String {
    A("1", "2").testAccessors()
    return "OK"
}

public fun <T, R> checkAccessor(prop: KMutableProperty1<T, R>, value: R, newValue: R, receiver: T) {
    assertEquals(prop.get(receiver), value, "Property ${prop} has wrong value")
    prop.set(receiver, newValue)
    assertEquals(prop.get(receiver), newValue, "Property ${prop} has wrong value")
}
