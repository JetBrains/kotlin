<!DIRECTIVES("HELPERS: REFLECT")!>

typealias <!ELEMENT(1)!> = Boolean

internal typealias <!ELEMENT(2)!><<!ELEMENT(3)!>> = Map<<!ELEMENT(3)!>, List<<!ELEMENT(3)!>>?>

fun box(): String? {
    val x1: <!ELEMENT(2)!><Boolean> = mapOf(true to listOf(false, false), false to null)
    val x2: <!ELEMENT(1)!> = false

    if (!x1[true]!!.containsAll(listOf(false, false)) || x1[false] != null) return null
    if (x2) return null

    if (!checkClassName(<!ELEMENT(1)!>::class, "kotlin.Boolean")) return null

    return "OK"
}