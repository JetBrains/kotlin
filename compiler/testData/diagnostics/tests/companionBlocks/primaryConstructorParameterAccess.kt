// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
// ISSUE: KT-85679
class B(p: String, l: Lazy<String>) {
    companion {
        val a = <!UNRESOLVED_REFERENCE!>p<!>
        val b by <!UNRESOLVED_REFERENCE!>l<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, objectDeclaration, primaryConstructor, propertyDeclaration,
propertyDelegate, starProjection */
