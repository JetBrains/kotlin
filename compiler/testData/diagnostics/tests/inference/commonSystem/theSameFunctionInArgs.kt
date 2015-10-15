// !CHECK_TYPE

@Suppress("UNCHECKED_CAST")
fun <T> arrayOf(vararg t : T) : Array<T> = t as Array<T>

fun test() {
    val array = arrayOf(arrayOf(1))
    array checkType { _<Array<Array<Int>>>() }
}