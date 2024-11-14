// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
fun test(func: (() -> Unit)?) {
    if (func != null) {
        func()
    }
}
