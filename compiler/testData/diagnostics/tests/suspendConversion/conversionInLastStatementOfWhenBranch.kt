// ISSUE: KT-69473

fun test(b: Boolean) {
    val f: (suspend (String) -> String)? =
        if (b) {
            { s: String -> s }
        } else {
            null
        }
}
