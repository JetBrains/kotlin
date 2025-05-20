// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun <T> select(x: T, y: T): T = x
open class Inv<K>
class SubInv<V> : Inv<V>()

fun testSimple() {
    val a0 = select(Inv<Int>(), SubInv())

    a0

    val a1 = select(SubInv<Int>(), Inv())

    a1
}

fun testNullability() {
    val n1 = select(Inv<Int?>(), SubInv())

    n1

    val n2 = select(SubInv<Int?>(), Inv())

    n2
}

fun testNested() {
    val n1 = select(Inv<Inv<Int>>(), SubInv())

    n1

    val n2 = select(SubInv<SubInv<Int>>(), Inv())

    n2

    fun <K> createInvInv(): Inv<Inv<K>> = TODO()

    val n3 = select(SubInv<SubInv<Int>>(), createInvInv())

    n3
}

fun testCaptured(cSub: SubInv<out Number>, cInv: Inv<out Number>) {
    val c1 = select(cInv, SubInv())

    c1

    val c2 = select(cSub, Inv())

    c2
}

fun testVariableWithBound() {
    fun <K : Number> createWithNumberBound(): Inv<K> = TODO()
    fun <K : <!FINAL_UPPER_BOUND!>Int<!>> createWithIntBound(): Inv<K> = TODO()

    val c1 = select(SubInv<Int>(), createWithNumberBound())

    c1

    val c2 = <!TYPE_MISMATCH("Number; String")!>select(SubInv<String>(), createWithNumberBound())<!>

    c2

    val c3 = <!TYPE_MISMATCH("Int; Double")!>select(SubInv<Double>(), createWithIntBound())<!>

    c3
}

fun testCapturedVariable() {
    fun <K> createInvOut(): Inv<out K> = TODO()
    fun <V> createSubInvOut(): SubInv<out V> = TODO()

    fun <K> createInvIn(): Inv<in K> = TODO()

    val c1 = select(SubInv<Number>(), createInvOut())

    c1

    val c2 = select(createSubInvOut<Number>(), createInvOut())

    c2

    val c3 = <!TYPE_MISMATCH("Inv<out Number>; Inv<CapturedType(in Number)>")!>select(SubInv<Number>(), createInvIn())<!>

    c3
}
