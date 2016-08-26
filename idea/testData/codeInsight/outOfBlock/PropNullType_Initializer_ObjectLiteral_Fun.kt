// TRUE

// Problem with lazy initialization of nullable properties
interface Some

val test: Some? = object: Some {
    fun test() {
        <caret>
    }
}

// SKIP_ANALYZE_CHECK