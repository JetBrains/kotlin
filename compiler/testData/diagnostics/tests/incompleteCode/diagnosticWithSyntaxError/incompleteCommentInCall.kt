// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

class Box(val value: String)

fun main() {
    val box = Box("")
    O.foo(
        aaaa = box.value,
        <!SYNTAX!><!SYNTAX!><!>/<!>
    <!UNRESOLVED_REFERENCE!>bbbb<!> = false
    <!SYNTAX!>)<!>
}

object O {
    fun foo(aa: String, aaaa: String, bbbb: Boolean) {}
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, localProperty, objectDeclaration,
primaryConstructor, propertyDeclaration, stringLiteral */
