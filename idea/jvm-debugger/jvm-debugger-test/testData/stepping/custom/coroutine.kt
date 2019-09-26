package coroutine

import forTests.builder

suspend fun second() {
}

suspend fun first() {
    second()
}

fun main(args: Array<String>) {
    // SMART_STEP_INTO_BY_INDEX: 2
    //Breakpoint!
    builder {
        first()
    }
}

// STEP_OVER: 1

// TODO: Breakpoint on builder {} is now triggered twice. This is because generated line number on suspend function enter.