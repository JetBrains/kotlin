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
    val content = listOf("foo", "bar", "bar")
    val expected = listOf("bar", "bar")

    testOperation(content, expected, true).let { assertRetain ->
        val data = listOf("bar")
        assertRetain { retainAll(data) }
        assertRetain { retainAll(data.toTypedArray()) }
        assertRetain { retainAll(data.toTypedArray().asIterable()) }
        assertRetain { retainAll(data.asSequence()) }
        assertRetain { retainAll { it in data } }
        assertRetain { (this as MutableIterable<String>).retainAll { it in data } }

        val predicate = { cs: CharSequence -> cs.first() == 'b' }
        assertRetain { retainAll(predicate) }
    }
    testOperation(content, emptyList(), true).let { assertRetain ->
        val data = emptyList<String>()
        assertRetain { retainAll(data) }
        assertRetain { retainAll(data.toTypedArray()) }
        assertRetain { retainAll(data.toTypedArray().asIterable()) }
        assertRetain { retainAll(data.asSequence()) }
        assertRetain { retainAll { it in data } }
        assertRetain { (this as MutableIterable<String>).retainAll { it in data } }
    }
    testOperation(emptyList<String>(), emptyList(), false).let { assertRetain ->
        val data = emptyList<String>()
        assertRetain { retainAll(data) }
        assertRetain { retainAll(data.toTypedArray()) }
        assertRetain { retainAll(data.toTypedArray().asIterable()) }
        assertRetain { retainAll(data.asSequence()) }
        assertRetain { retainAll { it in data } }
        assertRetain { (this as MutableIterable<String>).retainAll { it in data } }
    }
}
