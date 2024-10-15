// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL

class Dup {
    fun String.Dup() : Unit {
        this<!AMBIGUOUS_LABEL!>@Dup<!>
    }
}

