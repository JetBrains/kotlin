// DIAGNOSTICS: -UNUSED_PARAMETER
// Issue: KT-30300

class Inv<T>
class InvOut<T, out K>

class Sample

fun <T> select(x: T, y: T): T = x
fun <K1, V1> selectInvOut(a: InvOut<out K1, V1>, b: InvOut<out K1, V1>): InvOut<K1, V1> = TODO()
fun <K2, V2> emptyInvOut(): InvOut<K2, V2> = TODO()
fun <S> create(element: S): InvOut<Inv<S>, S> = TODO()

fun test(s: Sample, b: InvOut<Inv<*>, Any?>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("InvOut<Inv<*>, kotlin.Any?>")!>selectInvOut(
        b,
        select(create(s), emptyInvOut())
    )<!>
}

