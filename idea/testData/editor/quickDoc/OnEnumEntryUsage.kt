enum class TestEnum{
    A, B, C
}

fun test() {
    TestEnum.<caret>C
}

//INFO: <div class='definition'><pre><a href="psi_element://TestEnum"><code>TestEnum</code></a><br>enum entry <b>C</b>Enum constant ordinal: 2</pre></div>
