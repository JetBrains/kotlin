class C {
    /** Use [SOME_REFERENCED_VAL] to do something */
    fun fo<caret>o() {

    }

    companion object {
        val SOME_REFERENCED_VAL = 1
    }
}

//INFO: <div class='definition'><pre><a href="psi_element://C"><code>C</code></a><br>public final fun <b>foo</b>(): Unit</pre></div><div class='content'><p>Use <a href="psi_element://SOME_REFERENCED_VAL">SOME_REFERENCED_VAL</a> to do something</p></div><table class='sections'></table>
