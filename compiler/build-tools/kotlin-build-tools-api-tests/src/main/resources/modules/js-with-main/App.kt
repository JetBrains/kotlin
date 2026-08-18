fun usedByMain() {
    println("usedByMainMarker")
}

fun deadUnusedFunction() {
    println("deadUnusedFunctionMarker")
}

fun main() {
    usedByMain()
}
