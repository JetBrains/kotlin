// WITH_RUNTIME
class Api(val name: String)

fun test(api: Api) {
    val name = if (api.name.isBlank<caret>()) "John" else api.name
}