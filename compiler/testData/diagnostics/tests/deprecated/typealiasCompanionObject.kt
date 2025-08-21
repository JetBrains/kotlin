// RUN_PIPELINE_TILL: BACKEND
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

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, propertyDeclaration,
stringLiteral, typeAliasDeclaration */
