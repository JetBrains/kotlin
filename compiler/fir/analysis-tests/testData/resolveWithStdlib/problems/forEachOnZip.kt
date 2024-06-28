// ISSUE: KT-60450
fun main() {
    val l = listOf<String>()
    l.zip(l).forEach { left, <!CANNOT_INFER_PARAMETER_TYPE!>right<!><!SYNTAX!><!>

    }
    l.zip(l).forEach <!ARGUMENT_TYPE_MISMATCH!>{ left, <!CANNOT_INFER_PARAMETER_TYPE!>right<!> ->

    }<!>
}
