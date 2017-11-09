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

//INFO: <pre><b>public</b> <b>open</b> <b>val</b> foo: Int <i>defined in</i> D</pre><p>This property returns zero.</p>
