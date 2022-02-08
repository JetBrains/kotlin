// WITH_REFLECT
// TARGET_BACKEND: JVM
import kotlin.reflect.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.full.getExtensionDelegate

class C(var x: Int) {
    var y by C::x
    var z by ::x
}

class D(val c: C) {
    var y by c::x
    var C.w by C::x
    var Int.q by Int::x
}

var x = 1
var y by ::x
var z by C(1)::x

var _x = -99
var Int.x
    get() = this + _x
    set(v: Int) { _x = v - this }
var Int.y by Int::x

inline fun <P : KProperty<*>, R> P.test(delegate: P.() -> R, get: R.() -> Int, set: R.(Int) -> Unit) {
    val ref = apply { isAccessible = true }.delegate()
    require(ref.get() == 1) { "$ref initial value is ${ref.get()}, not 1" }
    ref.set(2)
    require(ref.get() == 2) { "$ref after set(2) is ${ref.get()}, not 2" }
    ref.set(1)
}

fun KMutableProperty0<Int>.test() =
    test({ getDelegate() as KMutableProperty0<Int> }, { get() }, { set(it) })

fun <T> KMutableProperty1<T, Int>.test(receiver: T) =
    test({ getDelegate(receiver) as KMutableProperty0<Int> }, { get() }, { set(it) })

fun <T> KMutableProperty1<T, Int>.test(receiver: T, receiver2: T) =
    test({ getDelegate(receiver) as KMutableProperty1<T, Int> }, { get(receiver2) }, { set(receiver2, it) })

fun <R1, R2> KMutableProperty2<R1, R2, Int>.test(receiver1: R1, receiver2: R2) =
    test({ getDelegate(receiver1, receiver2) as KMutableProperty1<R2, Int> }, { get(receiver2) }, { set(receiver2, it) })

fun box(): String {
    C::y.test(C(100), C(1))
    C::z.test(C(1))
    D::y.test(D(C(1)))
    ::y.test()
    ::z.test()
    Int::y.test({ getExtensionDelegate() as KMutableProperty1<Int, Int> }, { get(100) }, { set(100, it) })

    val w = D::class.members.single { it.name == "w" } as KMutableProperty2<D, C, Int>
    w.test(D(C(100)), C(1))

    val q = D::class.members.single { it.name == "q" } as KMutableProperty2<D, Int, Int>
    q.test({ getExtensionDelegate(D(C(100))) as KMutableProperty1<Int, Int> }, { get(100) }, { set(100, it) })

    return "OK"
}
