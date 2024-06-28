// WITH_STDLIB
// FIR_IDENTICAL
// ISSUE: KT-69040

fun main1() {
    build { ids ->
        val f: Set<String> = ids
        mapOf(
            "" to run {
                f.first { true }
            }
        )

    }
}

fun main2() {
    build { ids ->
        val f: Set<String> = ids
        mapOf(
            "" to ids.let {
                f.first { true }
            }
        )

    }
}

fun main3() {
    build { ids ->
        val f: Set<String> = ids
        mapOf(
            "" to ids.let {
                ids.first { true }
            }
        )

    }
}

fun main4() {
    build { ids ->
        val f: Set<String> = ids
        mapOf(
            "" to run {
                ids.first { true }
            }
        )

    }
}

fun <BK, BV> build(transformer: (Set<BK>) -> Map<BK, BV?>) {}
