// !LANGUAGE: +NewInference

import kotlin.test.assertEquals

fun test() {
    val u = when (true) {
        true -> 42
        else -> 1.0
    }

    <!TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING!>assertEquals<!>(42, u)
}