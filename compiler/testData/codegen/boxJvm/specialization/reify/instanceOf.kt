// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

fun <@JvmSpecialize reified T> instanceOf(x: Any?) = x is T

fun box(): String {
    if (!instanceOf<Int>(42)) return "fail: for instanceOf<Int>(Int)"
    if (instanceOf<Int>("abc")) return "fail: for instanceOf<Int>(String)"
    if (instanceOf<Int>(null)) return "fail: for instanceOf<Int>(null)"

    if (!instanceOf<Int?>(42)) return "fail: for instanceOf<Int?>(Int)"
    if (instanceOf<Int?>("abc")) return "fail: for instanceOf<Int?>(String)"
    if (!instanceOf<Int?>(null)) return "fail: for instanceOf<Int?>(null)"

    if (!instanceOf<() -> Unit>({})) return "fail: for instanceOf<() -> Unit>({})"
    if (!instanceOf<(Int) -> Unit>({ it: Int -> Unit })) return "fail: for instanceOf<(Int) -> Unit>({ it: Int -> Unit })"

    if (!instanceOf<MutableMap.MutableEntry<Int, Int>?>(mutableMapOf(1 to 2).entries.elementAt(0))) return "fail: for MutableMap.MutableEntry"

    return "OK"
}
