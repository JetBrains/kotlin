// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals

class MyList<T>(val data: Array<out T>) {
    companion object {
        operator fun <K> of(vararg strs: K) = MyList(strs)
    }
}

fun <U> makeString(lst: MyList<U>): String {
    var res = ""
    for (e in lst.data) {
        res += e
    }
    return res
}

fun makeStringWithNullableAny(lst: MyList<Any?>): String {
    return makeString<Any?>(lst)
}

fun box(): String {
    val resString = makeString<String>(["1", "2", "3"])
    val resInt = makeString<Int>([1, 2, 3])
    val resAny = makeString<Any>([1, "2", '3'])
    val nonGenericRes = makeStringWithNullableAny(["1", "2", "3"])
    return when {
        resString != "123" -> "Fail#String"
        resInt != "123" -> "Fail#Int"
        resAny != "123" -> "Fail#Any"
        nonGenericRes != "123" -> "Fail#NonGeneric"
        else -> "OK"
    }
}
