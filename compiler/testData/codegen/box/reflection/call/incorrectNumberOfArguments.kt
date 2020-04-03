// !LANGUAGE: +FunctionReferenceWithDefaultValueAsOtherType
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty

var foo: String = ""

class A(private var bar: String = "") {
    fun getBar() = A::bar
}

object O {
    @JvmStatic
    private var baz: String = ""

    @JvmStatic
    fun getBaz() = (O::class.members.single { it.name == "baz" } as KMutableProperty<*>).apply { isAccessible = true }

    fun getGetBaz() = O::class.members.single { it.name == "getBaz" } as KFunction<*>
}

fun check(callable: KCallable<*>, vararg args: Any?) {
    val expected = callable.parameters.size
    val actual = args.size

    if (expected == actual) {
        throw AssertionError("Bad test case: expected and actual number of arguments should differ (was $expected vs $actual)")
    }

    val expectedExceptionMessage = "Callable expects $expected arguments, but $actual were provided."

    try {
        callable.call(*args)
        throw AssertionError("Fail: an IllegalArgumentException should have been thrown")
    } catch (e: IllegalArgumentException) {
        if (e.message != expectedExceptionMessage) {
            // This most probably means that we don't check number of passed arguments in reflection
            // and the default check from Java reflection yields an IllegalArgumentException, but with a not that helpful message
            throw AssertionError("Fail: an exception with an unrecognized message was thrown: \"${e.message}\"" +
                                 "\nExpected message was: $expectedExceptionMessage")
        }
    }
}

fun box(): String {
    check(::box, null)
    check(::box, "")

    check(::A)
    check(::A, null, "")

    check(O.getGetBaz())
    check(O.getGetBaz(), null, "")


    val f = ::foo
    check(f, null)
    check(f, null, null)
    check(f, arrayOf<Any?>(null))
    check(f, "")

    check(f.getter, null)
    check(f.getter, null, null)
    check(f.getter, arrayOf<Any?>(null))
    check(f.getter, "")

    check(f.setter)
    check(f.setter, null, null)
    check(f.setter, null, "")


    val b = A().getBar()

    check(b)
    check(b, null, null)
    check(b, "", "")

    check(b.getter)
    check(b.getter, null, null)
    check(b.getter, "", "")

    check(b.setter)
    check(b.setter, null)
    check(b.setter, "")


    val z = O.getBaz()

    check(z)
    check(z, null, null)
    check(z, "", "")

    check(z.getter)
    check(z.getter, null, null)
    check(z.getter, "", "")

    check(z.setter)
    check(z.setter, null)
    check(z.setter, "")


    return "OK"
}
