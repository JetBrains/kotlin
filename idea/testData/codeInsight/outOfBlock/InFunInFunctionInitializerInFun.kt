// OUT_OF_CODE_BLOCK: TRUE

val a = 1
fun test() = if (a) {
    fun hello() {
        <caret>
    }
}
else {

}