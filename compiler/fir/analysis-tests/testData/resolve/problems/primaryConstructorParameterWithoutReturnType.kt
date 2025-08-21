// RUN_PIPELINE_TILL: FRONTEND
data class NoParameterNameAndReturnType(val<!SYNTAX!><!>)

data class <!CLASSIFIER_REDECLARATION!>NoParameterReturnTypeInTheMiddle<!>(val x: Int, val<!SYNTAX!><!>, val z: Int)

data class NoParameterReturnType(val x<!SYNTAX!><!>)

data class <!CLASSIFIER_REDECLARATION!>NoParameterReturnTypeInTheMiddle<!>(val x: Int, val y<!SYNTAX!><!>, val z: Int)

/* GENERATED_FIR_TAGS: classDeclaration, data, primaryConstructor, propertyDeclaration */
