// OUT_OF_CODE_BLOCK: FALSE

val test: Int? = if (true) {
    fun test() {
        val t<caret>
    }
}
else {

}