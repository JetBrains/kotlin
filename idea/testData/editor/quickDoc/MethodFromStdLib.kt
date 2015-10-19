fun test() {
    listOf(1, 2, 4).<caret>filter { it > 0 }
}

//INFO: <b>public</b> inline <b>fun</b> &lt;T&gt; Iterable&lt;T&gt;.filter(predicate: (T) &rarr; Boolean): List&lt;T&gt; <i>defined in</i> kotlin<p>Returns a list containing all elements matching the given <a href="psi_element://predicate">predicate</a>.</p>
