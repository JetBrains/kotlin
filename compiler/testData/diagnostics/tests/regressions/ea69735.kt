// RUN_PIPELINE_TILL: FRONTEND
enum class MyEnum {
    // Here we have a problem 
    // while checking on a deprecated super constructor
    FIRST<!SYNTAX!><!SYNTAX!><!>:<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
