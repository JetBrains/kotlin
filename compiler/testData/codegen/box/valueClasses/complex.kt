// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID, ANDROID_IR
// LANGUAGE: +ValueClasses, +ValueClassesSecondaryConstructorWithBody
// JVM_ABI_K1_K2_DIFF: KT-62582

@JvmInline
value class A<T : Any>(val x: List<T>)

@JvmInline
value class B(val x: UInt) {
    constructor(x: String) : this(x.toUInt()) {
        supply(x)
    }
}

@JvmInline
value class C(val x: Int, val y: B, val z: String)

@JvmInline
value class D(val x: C) {
    constructor(x: Int, y: UInt, z: Int) : this(C(x, B(y), z.toString())) {
        supply(y)
    }

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
    supply(e)
}

fun <T : List<Int>> h(r: R<T>) {
    g(r.z)
    f(r)
    r
    require(B("3") == B(3U))
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

val lines = mutableListOf<String>()

fun supply(x: Any) {
    lines.add(x.toString())
}

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

lateinit var late1: E

fun box(): String {
    supply("#1")
    require(inlined(1, 2U, 3) == D(C(1, B(2U), "3")))
    supply("#2")
    require(notInlined(1, 2U, 3) == D(C(1, B(2U), "3")))
    supply("#3")
    val e = E(D(3, 4U, 5))
    supply("#4")
    val r = R(1, 2U, e, A(listOf(listOf(6))))
    supply("#5")
    f(r)
    supply("#6")
    g(e)
    supply("#7")
    h(r)
    supply("#8")
    h1()
    supply("#9")
    val ni = NotInlined(r, 7)
    supply("#10")
    ni.withNonTrivialGettersWithBF
    supply("#11")
    ni.withNonTrivialSettersWithBF = ni.withNonTrivialSettersWithBF
    supply("#12")
    supply(ni.toString())
    supply("#13")
    testVars(ni)
    supply("#14")
    reuseBoxed(mutableListOf(r))
    supply("#15")
    equalsChecks(r, r)
    supply("#16")
    equalsChecks1(A(listOf(listOf())))
    supply("#17")
    late1 = e
    lateinit var late2: E
    late2 = e
    supply(e)
    supply(late1)
    supply(late2)
    supply("#18")

    val log = lines.joinToString("\n")
    val expectedLog =
        """
        #1
        1
        1
        #2
        1
        1
        #3
        3
        4
        #4
        #5
        R(x=1, y=2, z=E(x=D(x=C(x=3, y=B(x=4), z=5))), t=A(x=[[6]]))
        1
        2
        E(x=D(x=C(x=3, y=B(x=4), z=5)))
        A(x=[[6]])
        [[6]]
        D(x=C(x=3, y=B(x=4), z=5))
        C(x=3, y=B(x=4), z=5)
        3
        B(x=4)
        5
        4
        #6
        E(x=D(x=C(x=3, y=B(x=4), z=5)))
        #7
        E(x=D(x=C(x=3, y=B(x=4), z=5)))
        R(x=1, y=2, z=E(x=D(x=C(x=3, y=B(x=4), z=5))), t=A(x=[[6]]))
        1
        2
        E(x=D(x=C(x=3, y=B(x=4), z=5)))
        A(x=[[6]])
        [[6]]
        D(x=C(x=3, y=B(x=4), z=5))
        C(x=3, y=B(x=4), z=5)
        3
        B(x=4)
        5
        4
        3
        2
        2
        4
        D(x=C(x=4, y=B(x=5), z=1))
        6
        6
        7
        6
        6
        D(x=C(x=6, y=B(x=7), z=2))
        #8
        1
        D(x=C(x=1, y=B(x=2), z=3))
        4
        D(x=C(x=4, y=B(x=5), z=6))
        #9
        #10
        1
        #11
        1
        3
        4
        #12
        R(x=1, y=2, z=E(x=D(x=C(x=3, y=B(x=4), z=5))), t=A(x=[[6]]))5
        #13
        R(x=1, y=2, z=E(x=D(x=C(x=3, y=B(x=4), z=5))), t=A(x=[[6]]))
        #14
        #15
        true
        true
        true
        true
        false
        false
        false
        false
        false
        false
        true
        true
        #16
        #17
        E(x=D(x=C(x=3, y=B(x=4), z=5)))
        E(x=D(x=C(x=3, y=B(x=4), z=5)))
        E(x=D(x=C(x=3, y=B(x=4), z=5)))
        #18
        """.trimIndent()
    require(log == expectedLog) { log }
    
    return "OK"
}
