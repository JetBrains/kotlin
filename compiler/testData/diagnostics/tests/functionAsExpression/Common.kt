// !DIAGNOSTICS: -UNUSED_PARAMETER

annotation class ann(val name: String)
val ok = "OK"

class A

val withName = fun name() {}
val extensionWithName = fun A.name() {}
val withoutName = fun () {}
val extensionWithoutName = fun A.() {}

fun withAnnotation() = [ann(ok)] fun () {}
val withReturn = fun (): Int { return 5}
val withExpression = fun() = 5
val funfun = fun() = fun() = 5

val parentesized = (fun () {})
val parentesizedWithType = (fun () {}) : () -> Unit
val withType = (fun () {}) : () -> Unit
