// PROBLEM: none
fun test(b: Boolean): Unit = when (b) {
    true -> {
        fun a() {}
        <caret>Unit
    }
    else -> {
    }
}