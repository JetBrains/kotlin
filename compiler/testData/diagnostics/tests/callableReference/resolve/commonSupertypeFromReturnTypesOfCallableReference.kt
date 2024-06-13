// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

interface Parent
interface Child1 : Parent
interface Child2 : Parent

fun foo(): Child1 = TODO()
fun bar(): Child2 = TODO()

fun <K> select(x: K, y: K): K = TODO()

fun test() {
    val a = select(::foo, ::bar)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction0<Parent>")!>a<!>
}