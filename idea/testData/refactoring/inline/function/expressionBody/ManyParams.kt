fun sideEffect() {
    println("qq")
}

fun effect(): String {
    sideEffect()
    return "effect"
}

fun manyParams(p1: String, p2: String, p3: String) = println(p1)

fun callManySimple() = <caret>manyParams(effect(), effect(), effect())