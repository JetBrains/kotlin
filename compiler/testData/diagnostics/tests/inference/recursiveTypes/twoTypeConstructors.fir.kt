// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun <S> select(vararg args: S): S = TODO()

interface Rec<T : Rec<T>>

interface I1
interface I2 : I1
interface I3 : I1

object O1 : Rec<O1>, I2
object O2 : Rec<O2>, I3

fun test() {
    val cst = select(O1, O2)
    <!DEBUG_INFO_EXPRESSION_TYPE("Rec<*> & I1")!>cst<!>
}