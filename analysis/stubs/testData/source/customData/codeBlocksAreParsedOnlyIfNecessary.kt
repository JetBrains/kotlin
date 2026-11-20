package foo

fun withoutDeclarations() {
    listOf(1).map { it + 1 }
    println("fun class")
    val a = Int::class
    val b = a:: /* comment */ class
}

fun withLocalClass() {
    class Local
}

fun withContract(x: Boolean) {
    contract { returns() implies x }
}

val lambda = { x: Int -> x + 1 }
