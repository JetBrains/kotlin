// TARGET_BACKEND: JVM
// WITH_STDLIB


// FILE: test.kt
fun box(): String {
    if (!checkSimpleDestructuring()) return ""
    // TODO:
    // if (!checkDestructuringInFor()) return ""
    if (!checkDestructuringInLambda()) return ""
    return "OK"
}

fun checkSimpleDestructuring(): Boolean {
    (val second, var g: String = first) = Pair("OK", "")
    return g == "OK"
}

//fun checkDestructuringInFor(): Boolean {
//    for ((first, second) in listOf(Pair("OK", ""))){
//        if (first!="OK") return false
//    }
//    return true
//}

fun checkDestructuringInLambda(): Boolean {
    return listOf(Pair("OK", "")).all { (val value = first) ->
        value == "OK"
    }
}