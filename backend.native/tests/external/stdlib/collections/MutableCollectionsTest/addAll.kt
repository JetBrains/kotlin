import kotlin.test.*

fun <T, C : MutableCollection<T>> testOperation(before: List<T>, after: List<T>, expectedModified: Boolean, toMutableCollection: (List<T>) -> C)
        = fun(operation: (C.() -> Boolean)) {
    val list = toMutableCollection(before)
    assertEquals(expectedModified, list.operation())
    assertEquals(toMutableCollection(after), list)
}

fun <T> testOperation(before: List<T>, after: List<T>, expectedModified: Boolean)
        = testOperation(before, after, expectedModified, { it.toMutableList() })

fun box() {
    val data = listOf("foo", "bar")

    testOperation(emptyList(), data, true).let { assertAdd ->
        assertAdd { addAll(data) }
        assertAdd { addAll(data.toTypedArray()) }
        assertAdd { addAll(data.toTypedArray().asIterable()) }
        assertAdd { addAll(data.asSequence()) }
    }

    testOperation(data, data, false, { it.toCollection(LinkedHashSet()) }).let { assertAdd ->
        assertAdd { addAll(data) }
        assertAdd { addAll(data.toTypedArray()) }
        assertAdd { addAll(data.toTypedArray().asIterable()) }
        assertAdd { addAll(data.asSequence()) }
    }
}
