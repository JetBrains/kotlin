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

//INFO: <b>public</b> <b>open</b> <b>fun</b> foo(): Int <i>defined in</i> D<p>This method returns zero.</p>
