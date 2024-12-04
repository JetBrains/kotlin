// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction0

fun box(): String {
    val s: String? = "OK"
    val t: Throwable? = Throwable("test", null)
    var thr1: KFunction2<String?, Throwable?, Throwable> = ::Throwable
    val z = thr1(s, t)
    if (z.message !== s) return "fail 1: ${z.message}"
    if (z.cause !== t) return "fail 2: ${z.cause}"

    var thr2: KFunction1<String?, Throwable> = ::Throwable

    val z2 = thr2(s)
    if (z2.message !== s) return "fail 3: ${z2.message}"
    if (z2.cause !== null) return "fail 4: ${z2.cause}"

    var thr3: KFunction1<Throwable?, Throwable> = ::Throwable
    val z3 = thr3(t)
    if (z3.message != "java.lang.Throwable: test") return "fail 5: ${z3.message}"
    if (z3.cause !== t) return "fail 6: ${z2.cause}"

    var thr4: KFunction0<Throwable> = ::Throwable
    val z4 = thr4()
    if (z4.message !== null) return "fail 7: ${z4.message}"
    if (z4.cause !== null) return "fail 8: ${z4.cause}"

    return z.message!!
}
