// "Inline type parameter" "true"

data class DC(val x: Int, val y: String) {
    fun <S : Int<caret>> foo() {
        val a: S = Int.MAX_VALUE
    }
}