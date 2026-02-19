// RUN_PIPELINE_TILL: BACKEND
package test

// val prop1: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop1 = "".get(0)<!>

/* GENERATED_FIR_TAGS: integerLiteral, propertyDeclaration, stringLiteral */
