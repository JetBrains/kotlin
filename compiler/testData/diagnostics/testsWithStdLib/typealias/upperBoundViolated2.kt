// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
class Base<K : List<CharSequence>>
typealias Alias<T> = Base<List<T>>
val a = <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING!><!OTHER_ERROR_WITH_REASON!>Alias<!><Any>()<!> // Also should be error

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
