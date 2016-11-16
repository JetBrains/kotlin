class C {
    /** Use [SOME_REFERENCED_VAL] to do something */
    fun fo<caret>o() {

    }

    companion object {
        val SOME_REFERENCED_VAL = 1
    }
}

//INFO: <pre><b>public</b> <b>final</b> <b>fun</b> foo(): Unit <i>defined in</i> C</pre><p>Use <a href="psi_element://SOME_REFERENCED_VAL">SOME_REFERENCED_VAL</a> to do something</p>
