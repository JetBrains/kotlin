// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(b: Boolean?, c: Boolean) {
    funWithExactlyOnceCallsInPlace {
        if (b == null) return

        try {
            when (b) {
                true -> {
                    println(1)
                    return
                }
                false -> {
                    println(2)
                    throw Exception()
                }
            }
        } catch (e: Exception) {
            if (c) {
                return@funWithExactlyOnceCallsInPlace
            } else {
                return
            }
        }
        println(3)
    }
    println(3)
}

// TESTCASE NUMBER: 2
fun case_2(b: Boolean?, c: Boolean) {
    funWithAtLeastOnceCallsInPlace {
        when (b) {
            true -> {
                println(1)
                return
            }
            else -> {
                if (b == null) {
                    funWithExactlyOnceCallsInPlace {
                        when {
                            c == true -> throw Exception()
                            else -> funWithAtMostOnceCallsInPlace { return }
                        }
                        println(3)
                    }
                    println(3)
                } else {
                    throw Exception()
                }
                println(3)
            }
        }
        println(3)
    }
    println(3)
}
