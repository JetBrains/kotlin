// WITH_RUNTIME

fun test() {
    val map = mutableMapOf<String, () -> Unit>()
    map.<caret>put("") label@{
        return@label
    }
}