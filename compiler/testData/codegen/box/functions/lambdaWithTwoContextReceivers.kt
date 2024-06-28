// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// ISSUE: KT-52887

interface SnarkRoute {
    context(PageContext)
    fun route(route: String, block: context(PageContext, SnarkRoute) () -> Unit)
}

context(PageContext)
fun SnarkRoute.pagesFrom() {
    route("") {
        get {

        }
    }
}

data class PageContext(val context: String)


fun SnarkRoute.get(foo: ()-> Unit) {}

fun box(): String {
    return "OK"
}
