open class C() {
    /**
     * This method returns zero.
     */
    open fun foo(): Int = 0
}

class D(): C() {
    override fun foo(): Int = 1
}


fun test() {
    D().f<caret>oo()
}

//INFO: <div class='definition'><pre><a href="psi_element://D"><code>D</code></a><br>public open fun <b>foo</b>(): Int</pre></div><div class='content'><p>This method returns zero.</p></div><table class='sections'></table>
