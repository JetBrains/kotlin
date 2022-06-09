// FIR_IDENTICAL
// WITH_STDLIB
// KT-47143

interface Container<PARAM: Any>

interface ContainerType<PARAM: Any, CONT: Container<PARAM>>

<!CONFLICTING_OVERLOADS!>fun <R: Any> doGet(ep: ContainerType<*, *>): String<!> = TODO()

<!CONFLICTING_OVERLOADS!>@JvmName("name")
fun <R: Any, PARAM: Any, CONT: Container<PARAM>> doGet(ep: ContainerType<PARAM, CONT>): String<!> = TODO()

fun main() {}
