@file:OptIn(ExperimentalVersionOverloading::class)

fun invokeVersionedBlock(block: () -> String): String = block()

inline fun crossinlineVersioned(
    @IntroducedAt("1") crossinline block: () -> String = { "O" },
): String = invokeVersionedBlock { block() }

inline fun noinlineVersioned(
    @IntroducedAt("1") noinline block: () -> String = { "K" },
): String = block()

fun box(): String {
    if (crossinlineVersioned() != "O") return "FAIL crossinline default"
    if (crossinlineVersioned { "C" } != "C") return "FAIL crossinline explicit"
    if (noinlineVersioned() != "K") return "FAIL noinline default"
    if (noinlineVersioned { "N" } != "N") return "FAIL noinline explicit"
    return "OK"
}
