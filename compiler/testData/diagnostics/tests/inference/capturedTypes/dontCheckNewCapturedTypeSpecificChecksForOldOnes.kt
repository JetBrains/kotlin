// FIR_IDENTICAL
// WITH_STDLIB
// KT-47143

interface Container<PARAM: Any>

interface ContainerType<PARAM: Any, CONT: Container<PARAM>>

fun <R: Any> doGet(ep: ContainerType<*, *>): String = TODO()

@JvmName("name")
fun <R: Any, PARAM: Any, CONT: Container<PARAM>> doGet(ep: ContainerType<PARAM, CONT>): String = TODO()

fun main() {}
