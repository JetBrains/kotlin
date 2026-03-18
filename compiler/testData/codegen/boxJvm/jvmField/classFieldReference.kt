// TARGET_BACKEND: JVM

// WITH_STDLIB

package zzz
import java.lang.reflect.Field
import kotlin.reflect.KProperty1
import kotlin.reflect.KProperty0
import kotlin.test.assertEquals

class A(val s1: String, val s2: String) {
    @JvmField public val publicField = s1;
    @JvmField internal val internalField = s2;

    fun testAccessors() {
        checkAccessor(A::publicField, s1, this)
        checkAccessor(A::internalField, s2, this)
    }
}


class AWithCompanion {
    companion object {
        @JvmField public val publicField = "1";
        @JvmField internal val internalField = "2";

        fun testAccessors() {
            checkAccessor(AWithCompanion.Companion::publicField, "1")
            checkAccessor(AWithCompanion.Companion::internalField, "2")
        }
    }
}

fun box(): String {
    A("1", "2").testAccessors()
    AWithCompanion.testAccessors()
    return "OK"
}

public fun <T, R> checkAccessor(prop: KProperty1<T, R>, value: R, receiver: T) {
    assertEquals(prop.get(receiver), value, "Property ${prop} has wrong value")
}

public fun <R> checkAccessor(prop: KProperty0<R>, value: R) {
    assertEquals(prop.get(), value, "Property ${prop} has wrong value")
}
