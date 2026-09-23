// ISSUE: KT-89451
// WITH_COROUTINES

suspend fun observeAsValue(f: (suspend () -> String)?) {
    if (f == null) return
    f().length
}

suspend fun <T : (() -> String)?> foo(f : T) {
    observeAsValue(f)
}

fun box(): String = helpers.runBlocking {
    foo(null)
    "OK"
}
