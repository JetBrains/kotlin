fun some() : String? = null

fun test() {
    val <caret>test = some()
}


//INFO: <div class='definition'><pre><b>val</b> test: String?</pre></div>
