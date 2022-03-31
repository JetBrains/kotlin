// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
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
        println(x.x)
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

@JvmInline
value class R<T : Any>(val x: Int, val y: UInt, val z: E, val t: A<T>)

fun <T : List<Int>> f(r: R<T>) {
    println(r)
    println(r.x)
    println(r.y)
    println(r.z)
    println(r.t)
    println(r.t.x)
    println(r.z.x)
    println(r.z.x.x)
    println(r.z.x.x.x)
    println(r.z.x.x.y)
    println(r.z.x.x.z)
    println(r.z.x.x.y.x)
}

fun g(e: E) {
}

fun <T : List<Int>> h(r: R<T>) {
    g(r.z)
    f(r)
    r
    C(2, B(3U), "") // todo fix box
    D(C(2, B(3U), ""))
    val x = D(C(2, B(3U), ""))
    var y = D(C(4, B(5U), "1"))
    println(y)
    y = D(C(6, B(7U), "2"))
    y = D(6, 7U, 2)
    y = inlined(6, 7U, 2)
    y = notInlined(6, 7U, 2)
    println(y)
}

fun h1() {
    var y = inlined(1, 2U, 3) // todo fix box
    println(y)
    y = inlined(4, 5U, 6)
    println(y)
}

class NotInlined(var l: R<List<Int>>, var y: Int) {
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
            println("1")
            field
            field.t
            field == field
            return field
        }
        set(value) {
            println("3")
            field = value
            field = field
            println("4")
        }

    val withNonTrivialGettersWithBF: R<List<Int>> = l
        get() {
            println("1")
            field
            field.t
            field == field
            return field
        }
}

fun ekeke(x: NotInlined) {
    x.l.toString()
    var y = x.l
    y.toString()
    y = x.l
    println(y)
    x.l = x.l
    x.l = R<List<Int>>(x.l.x, x.l.y, x.l.z, x.l.t)
}

// todo add default parameters

fun box() = "bad"//.also { h(R(1, 2U, E(D(C(3, B(4U), "5"))), A(listOf(listOf(6))))) }
