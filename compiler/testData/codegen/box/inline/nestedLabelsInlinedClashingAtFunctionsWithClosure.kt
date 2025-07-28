package foo

class State() {
    public var value: Int = 0
}

internal fun test(state: State) {
    @Suppress(
        "NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION", // K2/JVM-only error
        "NOT_YET_SUPPORTED_IN_INLINE", // K1/JVM-only error
    )
    inline fun test3() {
        inline fun test2() {
            inline fun test1() {
                loop@ for (i in 1..10) {
                    state.value++
                    if (i == 2) break@loop
                }
            }

            loop@ for (i in 1..10) {
                test1()
                if (i == 2) break@loop
            }
        }

        loop@ for (i in 1..10) {
            test2()
            if (i == 2) break@loop
        }
    }

    test3()
}

fun box(): String {
    val state = State()
    test(state)

    return when (state.value) {
        8 -> "OK"
        else -> "FAIL: state.value = ${state.value}"
    }
}
