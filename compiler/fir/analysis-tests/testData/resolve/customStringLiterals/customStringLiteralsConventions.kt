object properReceiver1 {
    operator fun buildLiteral(body: ImproperBuilder1.() -> Unit) {}
}

class ImproperBuilder1 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun appendString(s: Int) {}
}

object improperReceiver1 {
    operator fun buildLiteral(body: Int) = 0
}

class ImproperBuilder2 {}
object properReceiver2 {
    operator fun buildLiteral(body: ImproperBuilder2.() -> Unit) = {}
}

class ImproperBuilder3 {
    fun appendString(s: String) {}
    fun appendObject(x: Any) {}
}

object properReceiver3 {
    operator fun buildLiteral(body: ImproperBuilder3.() -> Unit) {}
}

object improperReceiver2 {
    fun buildLiteral(body: ImproperBuilder3.() -> Unit) {}
}

class ProperBuilder1 {
    operator fun appendString(s: String) {}
    operator fun appendObject(x: Any) {}
}

object improperReceiver3 {
    fun buildLiteral(body: ProperBuilder1.() -> Unit) {}
}

fun test () {
    properReceiver1"<!ARGUMENT_TYPE_MISMATCH!>some string<!>"
    <!ARGUMENT_TYPE_MISMATCH, ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>improperReceiver1"some string"<!>
    <!UNRESOLVED_REFERENCE!>properReceiver2"some string"<!>
    <!OPERATOR_MODIFIER_REQUIRED!>properReceiver3"some string"<!>
    <!ARGUMENT_TYPE_MISMATCH, ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>improperReceiver1"some string"<!>
    <!OPERATOR_MODIFIER_REQUIRED, OPERATOR_MODIFIER_REQUIRED!>improperReceiver2"some string"<!>
    <!OPERATOR_MODIFIER_REQUIRED!>improperReceiver3"some string"<!>
}