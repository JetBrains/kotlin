// IGNORE_BACKEND_FIR: JVM_IR
// https://youtrack.jetbrains.com/issue/KT-52236/Different-modality-in-psi-and-fir
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID, ANDROID_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

@JvmInline
value class A<T : Any>(val x: List<T>)

@JvmInline
value class B(val x: UInt)

@JvmInline
value class C(val x: Int, val y: B, val z: String = "3")

@JvmInline
value class D(val x: C) {
    constructor(x: Int, y: UInt, z: Int) : this(C(x, B(y), z.toString()))

    init {
        supply(x.x)
    }
}

inline fun inlined(x: Int, y: UInt, z: Int): D {
    return D(C(x, B(y), z.toString()))
}

fun notInlined(x: Int, y: UInt, z: Int) = D(C(x, B(y), z.toString()))

@JvmInline
value class E(val x: D) {
    var withNonTrivialSetters: D
        get() = TODO()
        set(_) = TODO()
}

interface Base3 {
    val z: E
}

@JvmInline
value class R<T : Any>(val x: Int, val y: UInt, override val z: E, val t: A<T>) : Base1, Base3

fun <T : List<Int>> f(r: R<T>) {
    supply(r)
    supply(r.x)
    supply(r.y)
    supply(r.z)
    supply(r.t)
    supply(r.t.x)
    supply(r.z.x)
    supply(r.z.x.x)
    supply(r.z.x.x.x)
    supply(r.z.x.x.y)
    supply(r.z.x.x.z)
    supply(r.z.x.x.y.x)
}

fun g(e: E) {
}

fun <T : List<Int>> h(r: R<T>) {
    g(r.z)
    f(r)
    r
    C(2, B(3U), "")
    D(C(2, B(3U), ""))
    val x = D(C(2, B(3U), ""))
    var y = D(C(4, B(5U), "1"))
    supply(y)
    y = D(C(6, B(7U), "2"))
    y = D(6, 7U, 2)
    y = inlined(6, 7U, 2)
    y = notInlined(6, 7U, 2)
    supply(y)
}

fun h1() {
    var y = inlined(1, 2U, 3) // todo fix box
    supply(y)
    y = inlined(4, 5U, 6)
    supply(y)
}

interface Base1 {
    val fakeOverrideMFVC: R<List<Int>>
        get() = TODO()
    val fakeOverrideRegular: Int
        get() = TODO()
}

interface Base2 {
    var l: R<List<Int>>
}

interface Base4<T> {
    var l: T
}

class NotInlined(override var l: R<List<Int>>, var y: Int) : Base1, Base2, Base4<R<List<Int>>> {
    override fun toString(): String = l.toString() + l.z.x.x.z

    init {
        l = l
    }

    fun trySetter() {
        l = l
    }

    var withNonTrivialSetters: R<List<Int>>
        get() = TODO()
        set(_) = TODO()

    var withNonTrivialSettersWithBF: R<List<Int>> = l
        get() {
            supply("1")
            field
            field.t
            field == field
            return field
        }
        set(value) {
            supply("3")
            field = value
            field = field
            supply("4")
        }

    val withNonTrivialGettersWithBF: R<List<Int>> = l
        get() {
            supply("1")
            field
            field.t
            field == field
            return field
        }
}

fun testVars(x: NotInlined) {
    x.l.toString()
    var y = x.l
    y.toString()
    y = x.l
    supply(y)
    x.l = x.l
    x.l = R<List<Int>>(x.l.x, x.l.y, x.l.z, x.l.t)
}

fun reuseBoxed(list: MutableList<R<List<Int>>>) {
    list.add(list.last())
}

fun supply(x: Any) {}

fun equalsChecks1(x: A<List<Int>>) {}
fun equalsChecks(left: R<List<Int>>, right: R<List<Int>>) {
    supply(left == right)
    supply(left as Any == right)
    supply(left == right as Any)
    supply(left as Any == right as Any)
    supply(null == right)
    supply(left == null)
    supply(null as Any? == right)
    supply(null as R<List<Int>>? == right)
    supply(left == null as Any?)
    supply(left == null as R<List<Int>>?)
    supply(left as R<List<Int>>? == right)
    supply(left == right as R<List<Int>>?)
}

// todo add default parameters

fun box() = "OK"
