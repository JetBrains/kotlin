// TARGET_BACKEND: JS
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// WITH_REFLECT

fun fn0() {}
fun fn1(x: Any) {}

val lambda0 = {} as () -> Unit
val lambda1 = { x: Any -> } as (Any) -> Unit

fun Any.extFun() {}

var Any.extProp: String
        get() = "extProp"
        set(x: String) {}

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

    assertNonJVM(f0 is Function1<*, *>) { "Failed: f0 is Function1<*, *>" }
    assertNonJVM(f1 is Function0<*>) { "Failed: f1 is Function0<*>" }

    assertNonJVM(lambda0 is Function1<*, *>) { "Failed: lambda0 is Function1<*, *>" }
    assertNonJVM(lambda1 is Function0<*>) { "Failed: lambda1 is Function0<*>" }

    assertNonJVM(localFun0 is Function1<*, *>) { "Failed: localFun0 is Function1<*, *>" }
    assertNonJVM(localFun1 is Function0<*>) { "Failed: localFun1 is Function0<*>" }

    return "OK"
}

fun assertNonJVM(value: Boolean, message: ()->String) {
    if(!value)
        throw Exception(message())
}