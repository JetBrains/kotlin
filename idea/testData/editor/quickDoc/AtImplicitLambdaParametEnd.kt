fun foo() {
    listOf(1).forEach {
        println(it<caret>)
    }
}

//INFO: <div class='definition'><pre><b>value-parameter</b> it: Int</pre></div>
