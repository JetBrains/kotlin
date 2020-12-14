// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun <T> select(x: T, y: T): T = x
open class Inv<K>
class SubInv<V> : Inv<V>()

fun testSimple() {
    val a0 = select(Inv<Int>(), SubInv())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>a0<!>

    val a1 = select(SubInv<Int>(), Inv())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>a1<!>
}

fun testNullability() {
    val n1 = select(Inv<Int?>(), SubInv())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int?>")!>n1<!>

    val n2 = select(SubInv<Int?>(), Inv())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int?>")!>n2<!>
}

fun testNested() {
    val n1 = select(Inv<Inv<Int>>(), SubInv())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<kotlin.Int>>")!>n1<!>

    val n2 = select(SubInv<SubInv<Int>>(), Inv())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<SubInv<kotlin.Int>>")!>n2<!>

    fun <K> createInvInv(): Inv<Inv<K>> = TODO()

    val n3 = select(SubInv<SubInv<Int>>(), createInvInv())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out Inv<kotlin.Int>>")!>n3<!>
}

fun testCaptured(cSub: SubInv<out Number>, cInv: Inv<out Number>) {
    val c1 = select(cInv, SubInv())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c1<!>

    val c2 = select(cSub, Inv())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c2<!>
}

fun testVariableWithBound() {
    fun <K : Number> createWithNumberBound(): Inv<K> = TODO()
    fun <K : <!FINAL_UPPER_BOUND!>Int<!>> createWithIntBound(): Inv<K> = TODO()

    val c1 = select(SubInv<Int>(), createWithNumberBound())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>c1<!>

    val c2 = <!TYPE_MISMATCH!>select<!>(SubInv<String>(), <!TYPE_MISMATCH!>createWithNumberBound<!>())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>")!>c2<!>

    val c3 = <!TYPE_MISMATCH!>select<!>(SubInv<Double>(), <!TYPE_MISMATCH!>createWithIntBound<!>())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Double>")!>c3<!>
}

fun testCapturedVariable() {
    fun <K> createInvOut(): Inv<out K> = TODO()
    fun <V> createSubInvOut(): SubInv<out V> = TODO()

    fun <K> createInvIn(): Inv<in K> = TODO()

    val c1 = select(SubInv<Number>(), createInvOut())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c1<!>

    val c2 = select(createSubInvOut<Number>(), createInvOut())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c2<!>

    val c3 = <!TYPE_MISMATCH!>select<!>(SubInv<Number>(), createInvIn())

    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Number>")!>c3<!>
}
