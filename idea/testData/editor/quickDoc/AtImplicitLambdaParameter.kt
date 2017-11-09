fun foo() {
    listOf(1).forEach {
        println(i<caret>t)
    }
}

//INFO: <pre><b>value-parameter</b> it: Int</pre>
