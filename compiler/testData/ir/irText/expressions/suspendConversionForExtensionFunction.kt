// SKIP_KT_DUMP

fun runMe() {
    val foo: String.(suspend () -> Unit) -> Unit = {}
    val f: () -> Unit = {}
    "".foo(f)
}
