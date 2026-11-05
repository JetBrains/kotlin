// RUN_PIPELINE_TILL: CODEGEN

@file:Suppress("INLINE_CLASS_DEPRECATED", <!ERROR_SUPPRESSION!>"INLINE_CLASS_DEPRECATED_ERROR"<!>)

inline class ICAny<T>(val value: T)

inline class ICArray<T>(val value: Array<T>)

inline class ICList<T>(val value: List<T>)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, typeParameter */
