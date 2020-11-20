// FIR_COMPARISON
fun hello(moParam : Int) : Int {
    val more = 12

    val test = object {
        val sss = mo<caret>
    }

    return 12
}

// EXIST: more
// EXIST: moParam