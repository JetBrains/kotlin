// DIAGNOSTICS: -DEBUG_INFO_MISSING_UNRESOLVED

interface Sup {
    fun test() {}
}

class Dup : Sup {
    fun String.Dup() : Unit {
        super<!UNRESOLVED_LABEL!>@Dup<!>.test()
    }
}

