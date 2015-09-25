// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER

annotation class ann(val name: String)
const val ok = "OK"

class A

val withoutName = fun () {}
val extensionWithoutName = fun A.() {}

fun withAnnotation() = @ann(ok) fun () {}
val withReturn = fun (): Int { return 5}
val withExpression = fun() = 5
val funfun = fun() = fun() = 5

val parentesized = (fun () {})
val parentesizedWithType = checkSubtype<() -> Unit>((fun () {}))
val withType = checkSubtype<() -> Unit>((fun () {}))
