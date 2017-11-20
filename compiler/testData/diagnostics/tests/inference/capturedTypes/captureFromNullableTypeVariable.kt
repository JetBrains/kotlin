// !WITH_NEW_INFERENCE
// !CHECK_TYPE

fun <T : Any> Array<T?>.filterNotNull(): List<T> = throw Exception()

fun test1(a: Array<out Int?>) {
    val list = a.filterNotNull()
    list checkType { _<List<Int>>() }
}

fun test2(vararg a: Int?) {
    val list = a.filterNotNull()
    list checkType { _<List<Int>>() }
}