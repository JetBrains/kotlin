fun test() {
    listOf(1, 2, 4).<caret>filter { it > 0 }
}

// INFO: kotlin.inline <b>public</b> <b>fun</b> &lt;T&gt; kotlin.Iterable&lt;T&gt;.filter(predicate: (T) &rarr; kotlin.Boolean): kotlin.List&lt;T&gt; <i>defined in</i> kotlin<br/><p>Returns a list containing all elements matching the given *predicate*<br/></p>
