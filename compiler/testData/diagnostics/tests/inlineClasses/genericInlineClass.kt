// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// SKIP_TXT

inline class ICAny<T>(val value: T)

inline class ICArray<T>(val value: Array<T>)

inline class ICList<T>(val value: List<T>)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, typeParameter */
