// RUN_PIPELINE_TILL: FRONTEND
fun <<!REDECLARATION!>T<!>, <!REDECLARATION!>T<!>> Pair() {}

class P<<!REDECLARATION!>T<!>, <!REDECLARATION!>T<!>> {}

val <<!REDECLARATION, TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T<!>, <!REDECLARATION, TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T<!>> <!OVERLOAD_RESOLUTION_AMBIGUITY!>T<!>.foo : Int
    get() = 1

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, typeParameter */
