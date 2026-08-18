// WITH_STDLIB

sealed interface State {
    data object Initial : State

    data class Updated(val value: Int) : State
}

fun proceed() = true

fun reachedByBreak(): Int {
    var state: State = State.Initial
    while (proceed()) {
        state = State.Updated(42)
        break
    }
    return when (val current = state) {
        State.Initial -> 0
        is State.Updated -> current.value
    }
}

fun reachedByContinue(): Int {
    var state: State = State.Initial

    for (shouldUpdate in listOf(true, false)) {
        if (shouldUpdate) {
            state = State.Updated(42)
            continue
        }

        return when (val current = state) {
            State.Initial -> 0
            is State.Updated -> current.value
        }
    }

    error("unreachable")
}

fun box(): String {
    val byBreak = reachedByBreak()
    if (byBreak != 42) return "fail: break variant returned $byBreak"

    val byContinue = reachedByContinue()
    if (byContinue != 42) return "fail: continue variant returned $byContinue"

    return "OK"
}
