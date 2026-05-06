// RUN_PIPELINE_TILL: FRONTEND
val thingWithDelefate by lazy { 2 as Number }
    <!BACKING_FIELD_FOR_DELEGATED_PROPERTY!>field<!> = 10

val delegatedEBF: List<String>
    <!EXPLICIT_FIELD_MUST_BE_INITIALIZED!>field: MutableList<String><!> <!SYNTAX!>by lazy<!> <!FUNCTION_DECLARATION_WITH_NO_NAME!><!SYNTAX!><!>{ mutableListOf<String>() }<!>

/* GENERATED_FIR_TAGS: asExpression, explicitBackingField, integerLiteral, lambdaLiteral, nullableType,
propertyDeclaration, propertyDelegate */
