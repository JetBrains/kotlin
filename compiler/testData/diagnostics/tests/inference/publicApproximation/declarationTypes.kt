// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION

interface Bound1
interface Bound2 
object First : Bound1, Bound2
object Second : Bound1, Bound2

fun <S : Bound1> intersect(vararg elements: S): S = TODO()

fun topLevelFunction() = intersect(First, Second)
val Any.extensionProperty
    get() = intersect(First, Second)

fun Any.extensionFunction() = intersect(First, Second)

class Cls {
    val publicProperty = intersect(First, Second)
    private val privateProperty = intersect(First, Second)

    fun publicMemberFunction() = intersect(First, Second)
    private fun privateMemberFunction() = intersect(First, Second)

    fun testLocalDeclarations() {
        val localVariable = intersect(First, Second)
        fun localFunction() = intersect(First, Second)
        <!DEBUG_INFO_EXPRESSION_TYPE("{Bound1 & Bound2}")!>localVariable<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>privateProperty<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>localFunction()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>privateMemberFunction()<!>
    }
}

object Obj {
    val publicProperty = intersect(First, Second)
    private val privateProperty = intersect(First, Second)

    fun publicMemberFunction() = intersect(First, Second)
    private fun privateMemberFunction() = intersect(First, Second)

    fun testLocalDeclarations() {
        val localVariable = intersect(First, Second)
        fun localFunction() = intersect(First, Second)
        <!DEBUG_INFO_EXPRESSION_TYPE("{Bound1 & Bound2}")!>localVariable<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>localFunction()<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>privateProperty<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>privateMemberFunction()<!>
    }
}

fun test(cls: Cls, obj: Obj) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>topLevelFunction()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>Unit.extensionProperty<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>Unit.extensionFunction()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>cls.publicProperty<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>cls.publicMemberFunction()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>obj.publicProperty<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Bound1")!>obj.publicMemberFunction()<!>
}
