// FIR_IDENTICAL
// FIR_DUMP
// ISSUE: KT-68940

fun test() {
    build { ids ->
        val f: Set<String> = ids
        ids.associateWith { p -> f.first { true } }
    }
}

fun <BK, BV> build(
    transformer: (Set<BK>) -> Map<BK, BV?>,
) {}
