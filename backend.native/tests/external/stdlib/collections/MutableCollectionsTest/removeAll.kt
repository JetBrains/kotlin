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
    val data = listOf("bar")
    val expected = listOf("foo")

    testOperation(content, expected, true).let { assertRemove ->
        assertRemove { removeAll(data) }
        assertRemove { removeAll(data.toTypedArray()) }
        assertRemove { removeAll(data.toTypedArray().asIterable()) }
        assertRemove { removeAll { it in data } }
        assertRemove { (this as MutableIterable<String>).removeAll { it in data } }
        val predicate = { cs: CharSequence -> cs.first() == 'b' }
        assertRemove { removeAll(predicate) }
    }


    testOperation(content, content, false).let { assertRemove ->
        assertRemove { removeAll(emptyList()) }
        assertRemove { removeAll(emptyArray()) }
        assertRemove { removeAll(emptySequence()) }
        assertRemove { removeAll { false } }
        assertRemove { (this as MutableIterable<String>).removeAll { false } }
    }
}
