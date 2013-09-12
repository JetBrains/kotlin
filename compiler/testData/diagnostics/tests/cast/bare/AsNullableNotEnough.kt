trait Tr
trait G<T>

fun test(tr: Tr) {
    val v = tr as <!NO_TYPE_ARGUMENTS_ON_RHS!>G?<!>
    // If v is not nullable, there will be a warning on this line:
    v!!: G<*>
}