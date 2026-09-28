// ISSUE: KT-89451
// WITH_COROUTINES
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: ANY:2.0,2.1,2.2,2.3,2.4
class C {
    fun run() = ""
}

suspend fun observeAsValue(f: (suspend () -> String)?) {
    if (f == null) return
    f().length
}


fun box(): String = helpers.runBlocking {
    val b: C? = null
    observeAsValue(if (b == null) null else b::run)
    "OK"
}
