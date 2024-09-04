// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun stringF(): String = ""
fun nsf(): String? = "null"

fun Any.consume(): Unit = Unit

fun lhs(map: MutableMap<String, String>) {
    map["a"] = stringF()
    map[stringF()] = "a"
    map[stringF()] = nsf()!!
}

fun nested(map: List<MutableMap<String, String>>) {
    map[0]["b"] = stringF()
    map[0][stringF()] = stringF()
}

fun test_1(cards: List<List<MutableList<String>>>) {
    cards[0][0][0] = stringF()
    cards[0][0][0]
}

fun test_3(cards: MutableList<String>) {
    cards[0] = stringF()
    cards[0]
}
