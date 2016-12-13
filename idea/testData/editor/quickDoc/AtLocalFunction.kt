fun context() {
    fun local() {

    }

    <caret>local()
}

//INFO: <pre><b>local</b> <b>final</b> <b>fun</b> local(): Unit</pre>
