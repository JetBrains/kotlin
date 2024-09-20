// TARGET_BACKEND: NATIVE
// ASSERTIONS_MODE: always-enable
// WITH_STDLIB

var messageAssertionCalled = false

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)

fun checkMessageAssetion(condition: Boolean): Boolean {
    messageAssertionCalled = false
    try {
        assert(condition) {
            condition.let {
                try {
                    assert(it)
                } catch (e: AssertionError) {
                    messageAssertionCalled = true
                }
            }
        }
    } catch (e: AssertionError) {
        return messageAssertionCalled && !condition
    }
    return condition
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)

fun checkConditionAssetion(condition: Boolean): Boolean {
    try {
        assert(condition.let { assert(it); true })
    } catch (e: AssertionError) {
        return !condition
    }
    return condition
}

fun box(): String {
    if (checkConditionAssetion(true) && checkConditionAssetion(false) &&
        checkMessageAssetion(true) && checkMessageAssetion(false))
        return "OK"
    return "FAIL"
}