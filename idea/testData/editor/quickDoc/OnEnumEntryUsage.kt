enum class TestEnum{
    A, B, C
}

fun test() {
    TestEnum.<caret>C
}

//INFO: <pre><b>enum entry</b> C <i>defined in</i> TestEnum</pre><b>Enum constant ordinal: 2</b>
