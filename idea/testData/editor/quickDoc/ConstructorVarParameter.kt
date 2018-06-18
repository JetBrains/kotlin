class C(var v: Int) {
    fun foo() {
        print(<caret>v)
    }
}

//INFO: <div class='definition'><pre><a href="psi_element://C"><code>C</code></a><br><b>public</b> <b>final</b> <b>var</b> v: Int</pre></div>
