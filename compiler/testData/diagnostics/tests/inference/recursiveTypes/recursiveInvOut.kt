// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

interface Rec<A: Rec<A, B>, out B>

fun <S> select(vararg args: S): S = TODO()

interface I1
interface I2 : I1
interface I3 : I1
interface I4

object Obj2 : Rec<Obj2, I2>
object Obj3 : Rec<Obj3, I3>
object Obj4 : Rec<Obj4, I4>

fun testOutOut() {
    val cst1 = select(Obj2, Obj3)
    val cst2 = select(Obj2, Obj4)
    <!DEBUG_INFO_EXPRESSION_TYPE("Rec<*, I1>")!>cst1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Rec<*, kotlin.Any>")!>cst2<!>
}
