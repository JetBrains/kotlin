// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import kotlin.reflect.KProperty

suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

class A(val x: String) {
    var isSetValueCalled = false
    var isProvideDelegateCalled = false
    var isMinusAssignCalled = false
    var isIncCalled = false
    operator suspend fun component1() = suspendThere(x + "K")
    // There is no reason to support these operators until suspend properties are supported
//    operator suspend fun getValue(thisRef: Any?, property: KProperty<*>) = suspendThere(x + "K")
//    operator suspend fun setValue(thisRef: Any?, property: KProperty<*>, value: String): Unit = suspendCoroutineUninterceptedOrReturn { x ->
//        if (value != "56") return@suspendCoroutineUninterceptedOrReturn Unit
//        isSetValueCalled = true
//        x.resume(Unit)
//        COROUTINE_SUSPENDED
//    }
//
//    operator suspend fun provideDelegate(host: Any?, p: Any): A = suspendCoroutineUninterceptedOrReturn { x ->
//        isProvideDelegateCalled = true
//        x.resume(this)
//        COROUTINE_SUSPENDED
//    }

    operator suspend fun plus(y: String) = suspendThere(x + y)
    operator suspend fun unaryPlus() = suspendThere(x + "K")

    operator suspend fun inc(): A = suspendCoroutineUninterceptedOrReturn { x ->
        isIncCalled = true
        x.resume(this)
        COROUTINE_SUSPENDED
    }

    operator suspend fun minusAssign(y: String): Unit = suspendCoroutineUninterceptedOrReturn { x ->
        if (y != "56") return@suspendCoroutineUninterceptedOrReturn Unit
        isMinusAssignCalled = true
        x.resume(Unit)
        COROUTINE_SUSPENDED
    }
// See KT-16221
//    operator suspend fun contains(y: String): Boolean = suspendCoroutineUninterceptedOrReturn { x ->
//        x.resume(y == "56")
//        COROUTINE_SUSPENDED
//    }

    operator suspend fun compareTo(y: String): Int = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume("56".compareTo(y))
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var a = A("O")

//suspend fun foo1() {
//    var x by a
//
//    if (x != "OK") throw RuntimeException("fail 1")
//
//    x = "56"
//
//    if (!a.isSetValueCalled || !a.isProvideDelegateCalled) throw RuntimeException("fail 2")
//}

suspend fun foo2() {
    val (y) = a
    if (y != "OK") throw RuntimeException("fail 3")
}

suspend fun foo3() {
    val y = a + "K"
    if (y != "OK") throw RuntimeException("fail 4")
}

suspend fun foo4() {
    val y = + a
    if (y != "OK") throw RuntimeException("fail 5")
}

suspend fun foo5() {
    a -= "56"
    if (!a.isMinusAssignCalled) throw RuntimeException("fail 6")
}

suspend fun foo6() {
    var y = a++
    if (!y.isIncCalled) throw RuntimeException("fail 7")
}

suspend fun foo7() {
    a.isIncCalled = false
    val y = ++a
    if (!y.isIncCalled) throw RuntimeException("fail 8")
}

//suspend fun foo8() {
//    if ("1" in a) throw RuntimeException("fail 9")
//    if (!("1" !in a)) throw RuntimeException("fail 9")
//
//    if ("56" in a) throw RuntimeException("fail 10")
//    if (!("56" !in a)) throw RuntimeException("fail 11")
//}

suspend fun checkCompareTo(v: String) = (a < v) == ("56" < v)

suspend fun foo9() {
    if (!checkCompareTo("55")) throw RuntimeException("fail 12")
    if (!checkCompareTo("56")) throw RuntimeException("fail 13")
    if (!checkCompareTo("57")) throw RuntimeException("fail 14")
}

fun box(): String {

    builder {
        //foo1()
        foo2()
        foo3()
        foo4()
        foo5()
        foo6()
        foo7()
        //foo8()
        foo9()
    }

    return "OK"
}
