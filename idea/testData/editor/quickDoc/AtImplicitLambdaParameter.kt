fun foo() {
    listOf(1).forEach {
        println(i<caret>t)
    }
}

//INFO: <div class='definition'><pre>value-parameter <b>it</b>: Int</pre></div>
