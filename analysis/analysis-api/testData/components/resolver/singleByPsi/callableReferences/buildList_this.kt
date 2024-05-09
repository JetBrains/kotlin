// WITH_STDLIB
fun test(): List<String> {
    return buildList {
        val m: (String) -> Boolean = <expr>this::add</expr>
        m("test")
    }
}