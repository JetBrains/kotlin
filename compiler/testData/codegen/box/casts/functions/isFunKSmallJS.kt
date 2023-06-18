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

    if (f0 !is Function1<*, *>) return "Failed: f0 is Function1<*, *>"
    if (f1 !is Function0<*>) return "Failed: f1 is Function0<*>"

    if (lambda0 !is Function1<*, *>) return "Failed: lambda0 is Function1<*, *>"
    if (lambda1 !is Function0<*>) return "Failed: lambda1 is Function0<*>"

    if (localFun0 !is Function1<*, *>) return "Failed: localFun0 is Function1<*, *>"
    if (localFun1 !is Function0<*>) return "Failed: localFun1 is Function0<*>"

    return "OK"
}
