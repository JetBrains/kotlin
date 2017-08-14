// WITH_RUNTIME

fun <T> id(x: T): T = x

fun foo(<warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Int) {
    foo(<error descr="[CONTRADICTION_IN_CONSTRAINT_SYSTEM] Contradictory requirements for type variable 'T':
fun <T> id(x: T): T
should be a subtype of: Int (for parameter 'x')
should be a supertype of: String (for parameter 'x')
">id(if (true) 1 else "")</error>)
}