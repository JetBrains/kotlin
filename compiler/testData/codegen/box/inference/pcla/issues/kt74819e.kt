// ISSUE: KT-74819
// WITH_STDLIB

fun foo(x: List<String>) =
    buildList {
        add("")
        addAll(flatMap { x })
    }

fun box(): String {
    val result = foo(listOf("alpha", "omega"))
    if (result[0] != "") return "FAIL 0: ${result[0]}"
    if (result[1] != "alpha") return "FAIL 1: ${result[1]}"
    if (result[2] != "omega") return "FAIL 2: ${result[2]}"
    return "OK"
}
