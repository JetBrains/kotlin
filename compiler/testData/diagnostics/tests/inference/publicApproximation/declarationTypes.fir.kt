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
        localVariable
        privateProperty
        localFunction()
        privateMemberFunction()
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
        localVariable
        localFunction()
        privateProperty
        privateMemberFunction()
    }
}

fun test(cls: Cls, obj: Obj) {
    topLevelFunction()
    Unit.extensionProperty
    Unit.extensionFunction()
    cls.publicProperty
    cls.publicMemberFunction()
    obj.publicProperty
    obj.publicMemberFunction()
}
