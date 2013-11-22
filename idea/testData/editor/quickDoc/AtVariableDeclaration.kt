fun some() : String? = null

fun test() {
    val <caret>test = some()
}


//INFO: <b>val</b> test: jet.String? <i>defined in</i> test