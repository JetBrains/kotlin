fun test() {
    listOf(1, 2, 4).<caret>filter { it > 0 }
}

//INFO: <pre><b>public</b> <b>inline</b> <b>fun</b> &lt;T&gt; <a href="psi_element://kotlin.collections.Iterable">Iterable</a>&lt;<a href="psi_element://kotlin.collections.filter.T">T</a>&gt;.filter(predicate: (<a href="psi_element://kotlin.collections.filter.T">T</a>) &rarr; Boolean): <a href="psi_element://kotlin.collections.List">List</a>&lt;<a href="psi_element://kotlin.collections.filter.T">T</a>&gt; <i>defined in</i> kotlin.collections <i>in file</i> CollectionsKt.class</pre><p>Returns a list containing only elements matching the given <a href="psi_element://predicate">predicate</a>.</p>
