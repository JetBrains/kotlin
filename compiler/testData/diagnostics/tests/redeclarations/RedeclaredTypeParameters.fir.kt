// RUN_PIPELINE_TILL: FRONTEND
fun <<!REDECLARATION!>T<!>, <!REDECLARATION!>T<!>> Pair() {}

class P<<!REDECLARATION!>T<!>, <!REDECLARATION!>T<!>> {}

val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY, REDECLARATION!>T<!>, <!INCORRECT_TYPE_PARAMETER_OF_PROPERTY, REDECLARATION!>T<!>> <!OVERLOAD_RESOLUTION_AMBIGUITY!>T<!>.foo : Int
    get() = 1

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, typeParameter */
