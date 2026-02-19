// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_MISSING_UNRESOLVED

interface Sup {
    fun test() {}
}

class Dup : Sup {
    fun String.Dup() : Unit {
        super<!AMBIGUOUS_LABEL!>@Dup<!>.test()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration */
