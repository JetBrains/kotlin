// WITH_RUNTIME

fun <C> test(
        c: String,
        collectCandidates: (String) -> String
) : List<C> = listOf(collectCandidates(c) as C)


fun first(a: List<String>) = a.first()

fun box(): String = first(test("OK") { it })