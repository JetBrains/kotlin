fun foo() {
    listOf(1).forEach {
        println(it<caret>)
    }
}

//INFO: <pre><b>value-parameter</b> it: Int <i>defined in</i> foo.&lt;anonymous&gt;</pre>
