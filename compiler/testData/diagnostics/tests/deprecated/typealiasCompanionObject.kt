// FIR_IDENTICAL
class Relevant {
    companion object {
        val value = ""
    }
}

@Deprecated("Use Relevant")
typealias Obsolete = Relevant

fun test1() = <!DEPRECATION!>Obsolete<!>
fun test2() = <!DEPRECATION!>Obsolete<!>.value
fun test3() = <!DEPRECATION!>Obsolete<!>.toString()
