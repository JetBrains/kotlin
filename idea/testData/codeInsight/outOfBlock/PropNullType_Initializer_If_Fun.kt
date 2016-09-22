// TRUE

// Problem with lazy initialization of nullable properties

val test: Int? = if (true) {
    fun test() {
        val t<caret>
    }
}
else {

}

// SKIP_ANALYZE_CHECK