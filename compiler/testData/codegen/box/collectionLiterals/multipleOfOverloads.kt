// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals

class MyList(val data: String) {
    companion object {
        operator fun of(vararg strs: String) = MyList("")
        operator fun of(s1: String, s2: String) = MyList("O")
        operator fun of(s1: String) = MyList("K")
    }
}

fun makeString(vararg lsts: MyList): String {
    var res = ""
    for (e in lsts) {
        res += e.data
    }
    return res
}

fun box(): String {
    return makeString(
        [],
        ["1", "2"],
        ["1", "2", "3"],
        ["1"],
        [],
    )
}
