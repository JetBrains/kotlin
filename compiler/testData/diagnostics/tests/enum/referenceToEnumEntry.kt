// RUN_PIPELINE_TILL: FRONTEND
enum class My { V }

fun test() {
    val ref = My::<!UNRESOLVED_REFERENCE!>V<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, propertyDeclaration */
