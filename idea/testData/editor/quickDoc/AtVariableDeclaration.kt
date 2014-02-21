fun some() : String? = null

fun test() {
    val <caret>test = some()
}


//INFO: <b>val</b> test: kotlin.String? <i>defined in</i> test