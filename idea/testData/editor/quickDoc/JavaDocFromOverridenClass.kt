class A : OverrideMe() {
    override fun <caret>overrideMe() {
    }
}


//INFO: <pre><b>protected</b> <b>open</b> <b>fun</b> overrideMe(): Unit <i>defined in</i> A</pre><DD><DL><DT><b>Description copied from class:</b>&nbsp;<a href="psi_element://OverrideMe"><code>OverrideMe</code></a><br>
//INFO:        Some comment
//INFO:      </DD></DL></DD><DD><DL><DT><b>Overrides:</b><DD><a href="psi_element://OverrideMe#overrideMe()"><code>overrideMe</code></a> in class <a href="psi_element://OverrideMe"><code>OverrideMe</code></a></DD></DL></DD>
