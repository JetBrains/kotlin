fun foo() {
    listOf(1).forEach {
        println(i<caret>t)
    }
}

//INFO: <div class='definition'><pre><b>value-parameter</b> it: Int</pre></div>
