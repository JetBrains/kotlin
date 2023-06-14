// WITH_REFLECT

fun fn0() {}
fun fn1(x: Any) {}

val lambda0 = {} as () -> Unit
val lambda1 = { x: Any -> } as (Any) -> Unit

fun Any.extFun() {}

class A {
    fun foo() {}
}

fun box(): String {
    val f0 = ::fn0 as Any
    val f1 = ::fn1 as Any

    val ef = Any::extFun as Any

    val afoo = A::foo

    fun local0() {}
    fun local1(x: Any) {}

    val localFun0 = ::local0 as Any
    val localFun1 = ::local1 as Any

    assertNonJVM(f0 is Function0<*>) { "Failed: f0 is Function0<*>" }
    assertNonJVM(f1 is Function1<*, *>) { "Failed: f1 is Function1<*, *>" }

    assertNonJVM(lambda0 is Function0<*>) { "Failed: lambda0 is Function0<*>" }
    assertNonJVM(lambda1 is Function1<*, *>) { "Failed: lambda1 is Function1<*, *>" }

    assertNonJVM(localFun0 is Function0<*>) { "Failed: localFun0 is Function0<*>" }
    assertNonJVM(localFun1 is Function1<*, *>) { "Failed: localFun1 is Function1<*, *>" }

    assertNonJVM(ef is Function1<*, *>) { "Failed: ef is Function1<*, *>" }

    assertNonJVM(afoo is Function1<*, *>) { "afoo is Function1<*, *>" }

    return "OK"
}

fun assertNonJVM(value: Boolean, message: ()->String) {
    if(!value)
        throw Exception(message())
}