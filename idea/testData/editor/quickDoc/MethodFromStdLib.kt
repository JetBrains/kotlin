fun test() {
    listOf(1, 2, 4).<caret>filter { it > 0 }
}

// INFO: <b>public</b> <b>fun</b> &lt;T> jet.Collection&lt;T&gt;.filter(predicate: (T) &rarr; jet.Boolean): jet.List&lt;T&gt; <i>defined in</i> kotlin<br/><p>Returns a list containing all elements which match the given *predicate*<br/></p>