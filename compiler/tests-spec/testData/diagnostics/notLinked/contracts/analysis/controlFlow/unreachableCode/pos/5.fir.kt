// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(b: Boolean?) {
    funWithExactlyOnceCallsInPlace {
        if (b == null) return

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
                            else -> funWithAtLeastOnceCallsInPlace { return }
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
