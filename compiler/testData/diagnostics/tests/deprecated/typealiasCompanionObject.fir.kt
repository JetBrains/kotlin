class Relevant {
    companion object {
        val value = ""
    }
}

@Deprecated("Use Relevant")
typealias Obsolete = Relevant

fun test1() = Obsolete
fun test2() = Obsolete.<!UNRESOLVED_REFERENCE!>value<!>
fun test3() = Obsolete.toString()
