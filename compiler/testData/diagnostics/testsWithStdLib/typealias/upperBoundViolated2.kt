// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
class Base<K : List<CharSequence>>
typealias Alias<T> = Base<List<T>>
val a = Alias<<!UPPER_BOUND_VIOLATED_WARNING("List<CharSequence>; List<Any>")!>Any<!>>() // Also should be error

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, propertyDeclaration, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeConstraint, typeParameter */
