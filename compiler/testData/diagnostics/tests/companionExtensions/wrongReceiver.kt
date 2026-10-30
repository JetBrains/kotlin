// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// RENDER_DIAGNOSTICS_FULL_TEXT
open class Foo
companion fun Foo.pip(): String = "pip"
class Other

class Bar : Foo() {
    fun inMember(): String = <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>pip<!>()
    companion {
        fun inBlock(): String = <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>pip<!>()
    }
}
fun qualifiedViaChild(): String = Bar.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>pip<!>()
fun qualifiedViaOther(): String = Other.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>pip<!>()
fun topLevel(): String = <!UNRESOLVED_REFERENCE!>pip<!>()
fun onInstance(f: Foo): String = f.<!UNRESOLVED_REFERENCE!>pip<!>()
companion fun Other.inOtherExtension(): String = <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>pip<!>()

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, stringLiteral */
