// WITH_STDLIB
// ISSUE: KT-80744

var result: String = "NOT_OK"

fun <T> log(value: T) { result = value as String }

internal fun <T> lookupAttribute(attribute: T): Unit = run {
    class Matches

    val memory = Matches() to 2
    memory.let { (_, _) -> log(attribute) }
}

fun box(): String {
    lookupAttribute("OK")
    return result
}
