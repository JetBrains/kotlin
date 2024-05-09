// WITH_STDLIB
fun test(): List<String> {
    val list = mutableListOf<String>()
    return buildList {
        val m: (String) -> Boolean = <expr>list::add</expr>
        m("test")
    }
}