// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

interface Rec<in A: Rec<A, B>, B>

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
    <!DEBUG_INFO_EXPRESSION_TYPE("Rec<Obj2 & Obj3, out I1>")!>cst1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Rec<Obj2 & Obj4, out kotlin.Any>")!>cst2<!>
}
