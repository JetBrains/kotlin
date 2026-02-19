// RUN_PIPELINE_TILL: FRONTEND
fun <<!REDECLARATION, REDECLARATION!>T<!>, <!REDECLARATION, REDECLARATION!>T<!>> Pair() {}

class P<<!REDECLARATION!>T<!>, <!REDECLARATION!>T<!>> {}

val <<!REDECLARATION, TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T<!>, <!REDECLARATION!>T<!>> T.foo : Int
    get() = 1

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, typeParameter */
