// !DUMP_CFG
fun test(func: (() -> Unit)?) {
    if (func != null) {
        func()
    }
}