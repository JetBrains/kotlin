// WITH_STDLIB

fun nestedLambdas() {
    listOf(1, 2, 3).forEach { a ->
        println("Level 1: $a")
        listOf(4, 5, 6).filter { b ->
            println("Level 2: $b")
            listOf(19, 20, 21).<expr>sumOf { c ->
                println("Level 3: $c")
                gif(a + b + c > 50) 1L else 0
            }</expr>.toInt() > 0
        }
    }
}
