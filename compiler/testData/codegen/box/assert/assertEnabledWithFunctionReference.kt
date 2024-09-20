// TARGET_BACKEND: NATIVE
// ASSERTIONS_MODE: always-enable
// WITH_STDLIB

var allCalled = false
var filterCalled = false
var errorMessageCalled = false

fun List<Int>.errorMessage(): String {
    errorMessageCalled = true
    return "These elements are odd: $this"
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)

fun test(x: List<Int>) {
    allCalled = false
    filterCalled = false
    errorMessageCalled = false
    try {
        assert(x.all { allCalled = true; it % 2 == 0 }, x.filter { filterCalled = true; it % 2 != 0 }::errorMessage)
    } catch (e: AssertionError) {}
}

fun box(): String {
    test(listOf(1, 2))
    if (!allCalled || !filterCalled || !errorMessageCalled)
        return "[1, 2]"
    test(listOf(2, 2))
    if (!allCalled || !filterCalled || errorMessageCalled)
        return "[2, 2]"
    test(emptyList())
    if (allCalled || filterCalled || errorMessageCalled)
        return "[]"
    return "OK"
}