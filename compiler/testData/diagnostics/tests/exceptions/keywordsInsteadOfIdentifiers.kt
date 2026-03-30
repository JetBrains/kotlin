// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57529

fun <<!SYNTAX!>break<!>> foo() {}

fun test(){
    foo<String>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, typeParameter */
