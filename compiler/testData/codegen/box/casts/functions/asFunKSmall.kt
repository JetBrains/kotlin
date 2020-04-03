// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun fn0() {}
fun fn1(x: Any) {}

inline fun asFailsWithCCE(operation: String, block: () -> Unit) {
    try {
        block()
    }
    catch (e: ClassCastException) {
        return
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should throw ClassCastException, got $e")
    }
    throw AssertionError("$operation: should throw ClassCastException, no exception thrown")
}

inline fun asSucceeds(operation: String, block: () -> Unit) {
    try {
        block()
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }
}

class MyFun: Function<Any>

fun box(): String {
    val f0 = ::fn0 as Any
    val f1 = ::fn1 as Any

    val myFun = MyFun() as Any

    asSucceeds("f0 as Function0<*>") { f0 as Function0<*> }
    asFailsWithCCE("f0 as Function1<*, *>") { f0 as Function1<*, *> }
    asFailsWithCCE("f1 as Function0<*>") { f1 as Function0<*> }
    asSucceeds("f1 as Function1<*, *>") { f1 as Function1<*, *> }

    asFailsWithCCE("myFun as Function0<*>") { myFun as Function0<*> }
    asFailsWithCCE("myFun as Function1<*, *>") { myFun as Function1<*, *> }


    return "OK"
}
