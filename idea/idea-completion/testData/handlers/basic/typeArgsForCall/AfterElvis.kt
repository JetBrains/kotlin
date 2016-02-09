fun foo(list: List<String>?) {
    val v = list ?: emp<caret>
}

// ELEMENT: emptyList
