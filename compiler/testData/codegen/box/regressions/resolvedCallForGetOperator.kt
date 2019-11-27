// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val targetNameLists: Map<String, String> = mapOf("1"         to "OK")

fun <T> id(t: T) = t
fun foo(argumentName: String?): String? = id(targetNameLists[argumentName])

fun box() = foo("1")
