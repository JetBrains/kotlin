// !WITH_NEW_INFERENCE
val test1 = { when (true) { true -> 1; else -> "" } }

val test2 = { { when (true) { true -> 1; else -> "" } } }

val test3: (Boolean) -> Any = { when (true) { true -> 1; else -> "" } }

val test4: (Boolean) -> Any? = { when (true) { true -> 1; else -> "" } }

fun println() {}

val test5 = {
    when (true) {
        true -> println()
        else -> println()
    }
}
