fun some() : String? = null

fun test() {
    val <caret>test = some()
}


// INFO: <b>val</b> test: String?