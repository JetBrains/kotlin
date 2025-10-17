// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals

class MyList(val data: Array<out String>) {
    companion object {
        operator fun of(vararg strs: String) = MyList(strs)
    }
}

fun makeString(lst: MyList): String {
    var res = ""
    for (e in lst.data) {
        res += e
    }
    return res
}

fun box(): String {
    return makeString(["O", "", "K"])
}
