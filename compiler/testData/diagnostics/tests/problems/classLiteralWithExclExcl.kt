// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47373

class B<T>
val any2 = <!UNRESOLVED_REFERENCE!><!NO_COMPANION_OBJECT!>B<!>!!<!><<!UNRESOLVED_REFERENCE!>T<!>>::class

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, classReference, nullableType, propertyDeclaration,
typeParameter */
