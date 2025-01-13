// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextReceivers

context(String) context(<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)
fun foo() {}

context(String) context(<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)
val bar: String get() = ""

context(String) context(<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)
class C

class D {
    context(<!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>) context(<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)
    constructor()
}
