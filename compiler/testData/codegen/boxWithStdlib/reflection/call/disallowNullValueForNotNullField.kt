import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.jvm.JvmStatic as static

class A {
    private var foo: String = ""
}

object O {
    private @static var bar: String = ""
}

class CounterTest<T>(t: T) {
    private var baz: String? = ""
    private var generic: T = t
}

fun box(): String {
    val p = A::class.memberProperties.single() as KMutableProperty1<A, String?>
    p.isAccessible = true
    try {
        p.setter.call(A(), null)
        return "Fail: exception should have been thrown"
    } catch (e: IllegalArgumentException) {}


    val o = O::class.memberProperties.single() as KMutableProperty1<O, String?>
    o.isAccessible = true
    try {
        o.setter.call(O, null)
        return "Fail: exception should have been thrown"
    } catch (e: IllegalArgumentException) {}


    val c = CounterTest::class.memberProperties.single { it.name == "baz" } as KMutableProperty1<CounterTest<*>, String?>
    c.isAccessible = true
    c.setter.call(CounterTest(""), null) // Should not fail, because CounterTest::baz is nullable
    val d = CounterTest::class.memberProperties.single { it.name == "generic" } as KMutableProperty1<CounterTest<*>, String?>
    d.isAccessible = true
    d.setter.call(CounterTest(""), null) // Also should not fail, because we can't be sure about nullability of 'generic'

    return "OK"
}
