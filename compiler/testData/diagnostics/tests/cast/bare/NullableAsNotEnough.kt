trait Tr
trait G<T>

fun test(tr: Tr?) {
    val v = tr as <!NO_TYPE_ARGUMENTS_ON_RHS!>G<!>
    v: G<*>
}