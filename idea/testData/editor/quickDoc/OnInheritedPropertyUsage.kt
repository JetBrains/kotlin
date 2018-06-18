open class C() {
    /**
     * This property returns zero.
     */
    open val foo: Int get() = 0
}

class D(): C() {
    override val foo: Int get() = 1
}


fun test() {
    D().f<caret>oo
}

//INFO: <div class='definition'><pre><a href="psi_element://D"><code>D</code></a><br><b>public</b> <b>open</b> <b>val</b> foo: Int</pre></div><div class='content'><p>This property returns zero.</p></div><table class='sections'></table>
