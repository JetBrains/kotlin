// ISSUE: KT-89451
// WITH_COROUTINES
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: ANY:2.0,2.1,2.2,2.3

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
