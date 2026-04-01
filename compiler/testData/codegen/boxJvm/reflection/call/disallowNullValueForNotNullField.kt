// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

class A {
    private var foo: String = ""
}

object O {
    @JvmStatic
    private var bar: String = ""
}

class CounterTest<T>(t: T) {
    private var baz: String? = ""
    private var generic: T = t
}

class C {
    companion object {
        private var z: String = ""

        fun getBoundZ() = this::z
    }
}

private var topLevelNotNull: String = ""
private var topLevelNullable: String? = ""

private fun checkThrows(block: () -> Unit) {
    try {
        block()
        throw AssertionError("Fail: exception should have been thrown")
    } catch (e: IllegalArgumentException) {}
}

fun box(): String {
    val p = A::class.memberProperties.single() as KMutableProperty1<A, String?>
    p.isAccessible = true
    checkThrows {
        p.setter.call(A(), null)
    }

    val o = O::class.memberProperties.single() as KMutableProperty1<O, String?>
    o.isAccessible = true
    checkThrows {
        o.setter.call(O, null)
    }

    val c = CounterTest::class.memberProperties.single { it.name == "baz" } as KMutableProperty1<CounterTest<*>, String?>
    c.isAccessible = true
    c.setter.call(CounterTest(""), null) // Should not fail, because CounterTest::baz is nullable
    val d = CounterTest::class.memberProperties.single { it.name == "generic" } as KMutableProperty1<CounterTest<*>, String?>
    d.isAccessible = true
    d.setter.call(CounterTest(""), null) // Also should not fail, because we can't be sure about nullability of 'generic'

    val z = C.Companion::class.memberProperties.single { it.name == "z" } as KMutableProperty1<C.Companion, String?>
    z.isAccessible = true
    checkThrows {
        z.setter.call(C, null)
    }

    val zz = C.getBoundZ() as KMutableProperty0<String?>
    zz.isAccessible = true
    checkThrows {
        zz.setter.call(null)
    }

    checkThrows {
        ::topLevelNotNull.apply { isAccessible = true }.setter.call(null)
    }
    ::topLevelNullable.apply { isAccessible = true }.setter.call(null)

    return "OK"
}
